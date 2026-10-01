package io.github.cponfick.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import kotlin.math.abs
import kotlin.math.sqrt

@Stable
class CanvasState {
  var screenToCord by mutableStateOf(AffineTransformationMatrix2.IDENTITY); private set
  var cordToScreen by mutableStateOf(screenToCord.inverse()); private set
  var tempCordToScreen by mutableStateOf(AffineTransformationMatrix2.IDENTITY); private set
  var currentHeight by mutableStateOf(0.0); private set
  var currentWidth by mutableStateOf(0.0); private set
  var showLabels by mutableStateOf(true)
  var hasMoved by mutableStateOf(false); private set
  val zoomPercent get() = ((tempCordToScreen.apply(Vec2(1.0, 0.0)).distance(tempCordToScreen.apply(Vec2(0.0, 0.0))) / 45.0) * 100).toInt().coerceAtLeast(1)
  var hoveredPointId by mutableStateOf<Int?>(null); private set
  var hoveredSegmentId by mutableStateOf<Int?>(null); private set
  var isHelpDismissed by mutableStateOf(false); private set
  var interactionMode by mutableStateOf(InteractionMode.ADD_POINTS)
    private set
  val isSelectionMode get() = interactionMode == InteractionMode.SELECT
  val points = mutableStateMapOf<Int, Vec2>()
  val segments = mutableStateMapOf<Int, SegmentEntity>()
  val selectedPoints = mutableStateSetOf<Int>()
  val selectedSegments = mutableStateSetOf<Int>()
  val algorithmResults = mutableStateMapOf<String, AlgorithmResult>()

  private data class SceneSnapshot(val points: Map<Int, Vec2>, val segments: Map<Int, SegmentEntity>, val pointsSelected: Set<Int>, val segmentsSelected: Set<Int>)
  private val undoStack = ArrayDeque<SceneSnapshot>(); private val redoStack = ArrayDeque<SceneSnapshot>()
  private var nextPointId = 0; private var nextSegmentId = 0
  private var drawingStart: Vec2? = null
  var segmentPreview by mutableStateOf<Pair<Vec2, Vec2>?>(null); private set
  private var lastPointerPosition = Offset.Zero
  private var initialTransform = AffineTransformationMatrix2.IDENTITY
  private var hasLaidOut = false
  private val minPixelsPerWorldUnit = .1; private val maxPixelsPerWorldUnit = 10000.0
  private val pointHitRadiusPixels = 12.0; private val segmentHitRadiusPixels = 10.0

  private fun snapshot() = SceneSnapshot(points.toMap(), segments.toMap(), selectedPoints.toSet(), selectedSegments.toSet())
  private fun rememberUndo() { undoStack.addLast(snapshot()); if (undoStack.size > 50) undoStack.removeFirst(); redoStack.clear() }
  private fun setTransform(t: AffineTransformationMatrix2) { tempCordToScreen=t; cordToScreen=t.copy(); screenToCord=t.inverse() }
  fun onSizeChanged(width: Double, height: Double) {
    if (width <= 0 || height <= 0 || (width == currentWidth && height == currentHeight)) return
    if (!hasLaidOut) { setTransform(AffineTransformationMatrix2.createScaling(45.0, -45.0).translate(width/2, height/2)); initialTransform=tempCordToScreen; hasLaidOut=true }
    else setTransform(tempCordToScreen.translate((width-currentWidth)/2, (height-currentHeight)/2))
    currentWidth=width; currentHeight=height
  }
  fun onPointerPress(position: Offset = lastPointerPosition) {
    lastPointerPosition=position; hasMoved=false
    if (interactionMode == InteractionMode.DRAW_SEGMENTS) { val p=screenToCord.apply(Vec2(position.x.toDouble(), position.y.toDouble())); drawingStart=p; segmentPreview=p to p }
  }
  fun onPointerMove(delta: Offset) {
    hasMoved=true
    if (interactionMode == InteractionMode.DRAW_SEGMENTS && drawingStart != null) {
      val p=screenToCord.apply(Vec2(lastPointerPosition.x.toDouble()+delta.x, lastPointerPosition.y.toDouble()+delta.y)); lastPointerPosition += delta; segmentPreview=drawingStart!! to p
    } else { lastPointerPosition += delta; setTransform(tempCordToScreen.translate(delta.x.toDouble(), delta.y.toDouble())) }
  }
  private fun hitPoint(position: Offset): Int? = points.entries.map { (id,p) -> id to cordToScreen.apply(p) }.filter { (_,p) -> (p.x-position.x)*(p.x-position.x)+(p.y-position.y)*(p.y-position.y) <= pointHitRadiusPixels*pointHitRadiusPixels }.minByOrNull { (_,p) -> (p.x-position.x)*(p.x-position.x)+(p.y-position.y)*(p.y-position.y) }?.first
  private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Double { val px=p.x.toDouble(); val py=p.y.toDouble(); val ax=a.x.toDouble(); val ay=a.y.toDouble(); val bx=b.x.toDouble(); val by=b.y.toDouble(); val dx=bx-ax; val dy=by-ay; val len2=dx*dx+dy*dy; if (len2==0.0) return sqrt((px-ax)*(px-ax)+(py-ay)*(py-ay)); val t=((px-ax)*dx+(py-ay)*dy)/len2; val u=t.coerceIn(0.0,1.0); val x=ax+u*dx; val y=ay+u*dy; return sqrt((px-x)*(px-x)+(py-y)*(py-y)) }
  private fun hitSegment(position: Offset): Int? = segments.entries.map { (id,s) -> id to distanceToSegment(position, cordToScreen.apply(s.start).let { Offset(it.x.toFloat(),it.y.toFloat()) }, cordToScreen.apply(s.end).let { Offset(it.x.toFloat(),it.y.toFloat()) }) }.filter { it.second <= segmentHitRadiusPixels }.minByOrNull { it.second }?.first
  fun updateHover(position: Offset) { hoveredPointId=hitPoint(position); hoveredSegmentId=if (hoveredPointId==null) hitSegment(position) else null }
  fun cancelPointerGesture() { drawingStart=null; segmentPreview=null; hasMoved=false }
  fun onPinchZoom(previousCentroid: Offset, currentCentroid: Offset, scale: Double) {
    if (!scale.isFinite() || scale <= 0.0) return
    val old = tempCordToScreen.apply(Vec2(1.0,0.0)).distance(tempCordToScreen.apply(Vec2(0.0,0.0)))
    if (!old.isFinite() || old <= 0.0) return
    val new = (old * scale).coerceIn(minPixelsPerWorldUnit, maxPixelsPerWorldUnit)
    val anchor = Vec2(previousCentroid.x.toDouble(), previousCentroid.y.toDouble())
    val world = screenToCord.apply(anchor)
    val scaled = tempCordToScreen.scale(new / old, new / old)
    val scaledAnchor = scaled.apply(world)
    setTransform(scaled.translate(
      currentCentroid.x.toDouble() - scaledAnchor.x,
      currentCentroid.y.toDouble() - scaledAnchor.y
    ))
    lastPointerPosition = currentCentroid
  }
  fun onPointerRelease(position: Offset, isPrimary: Boolean) {
    lastPointerPosition=position
    if (interactionMode == InteractionMode.DRAW_SEGMENTS) {
      val start=drawingStart; val end=screenToCord.apply(Vec2(position.x.toDouble(),position.y.toDouble())); drawingStart=null; segmentPreview=null
      if (isPrimary && start != null && hasMoved && cordToScreen.apply(start).distance(cordToScreen.apply(end)) > segmentHitRadiusPixels) { rememberUndo(); segments[nextSegmentId++]=SegmentEntity(nextSegmentId-1,start,end); algorithmResults.clear() }; return
    }
    if (position.x<0 || position.x>currentWidth || position.y<0 || position.y>currentHeight || !isPrimary || hasMoved) return
    if (isSelectionMode) { val p=hitPoint(position); val s=if(p==null) hitSegment(position) else null; if(p!=null || s!=null) { rememberUndo(); if(p!=null) { if(!selectedPoints.add(p)) selectedPoints.remove(p) } else { if(!selectedSegments.add(s!!)) selectedSegments.remove(s) }; algorithmResults.clear() } }
    else { rememberUndo(); points[nextPointId++]=screenToCord.apply(Vec2(position.x.toDouble(),position.y.toDouble())); algorithmResults.clear() }
  }
  fun onScroll(scrollDelta: Float, scrollFactor: Double, position: Offset=lastPointerPosition) { if(!scrollDelta.isFinite()||!scrollFactor.isFinite())return; lastPointerPosition=position; val old=tempCordToScreen.apply(Vec2(1.0,0.0)).distance(tempCordToScreen.apply(Vec2(0.0,0.0))); if(!old.isFinite()||old<=0)return; val new=(old*(1+scrollDelta.toDouble()*scrollFactor)).coerceIn(minPixelsPerWorldUnit,maxPixelsPerWorldUnit); val anchor=Vec2(position.x.toDouble(),position.y.toDouble()); val world=screenToCord.apply(anchor); val scaled=tempCordToScreen.scale(new/old,new/old); val sa=scaled.apply(world); setTransform(scaled.translate(anchor.x-sa.x,anchor.y-sa.y)) }
  fun clearScene() { if(points.isNotEmpty()||segments.isNotEmpty()||selectedPoints.isNotEmpty()||selectedSegments.isNotEmpty())rememberUndo(); points.clear();segments.clear();selectedPoints.clear();selectedSegments.clear();algorithmResults.clear();hoveredPointId=null;hoveredSegmentId=null }
  fun clearPoints() = clearScene()
  private fun restore(s:SceneSnapshot) { points.clear();points.putAll(s.points);segments.clear();segments.putAll(s.segments);selectedPoints.clear();selectedPoints.addAll(s.pointsSelected);selectedSegments.clear();selectedSegments.addAll(s.segmentsSelected);algorithmResults.clear() }
  fun undo(){undoStack.removeLastOrNull()?.let{redoStack.addLast(snapshot());restore(it)}}; fun redo(){redoStack.removeLastOrNull()?.let{undoStack.addLast(snapshot());restore(it)}}
  val canUndo get()=undoStack.isNotEmpty(); val canRedo get()=redoStack.isNotEmpty()
  fun resetView(){if(hasLaidOut){setTransform(initialTransform);hasMoved=false}}
  fun dismissHelp(){isHelpDismissed=true}; fun removeAlgorithmResult(id:String){algorithmResults.remove(id)}
  fun applySelectionMode(selection:Boolean){interactionMode=if(selection)InteractionMode.SELECT else InteractionMode.ADD_POINTS; if(!selection){selectedPoints.clear();selectedSegments.clear()};algorithmResults.clear()}
  fun changeInteractionMode(mode:InteractionMode){interactionMode=mode;drawingStart=null;segmentPreview=null;algorithmResults.clear()}
  fun toggleSelectionMode()=applySelectionMode(!isSelectionMode)
  fun clearSelection(){if(selectedPoints.isNotEmpty()||selectedSegments.isNotEmpty())rememberUndo();selectedPoints.clear();selectedSegments.clear();algorithmResults.clear()}
  fun selectAllPoints(){if(selectedPoints!=points.keys)rememberUndo();selectedPoints.clear();selectedPoints.addAll(points.keys);algorithmResults.clear()}
  fun selectAllSegments(){if(selectedSegments!=segments.keys)rememberUndo();selectedSegments.clear();selectedSegments.addAll(segments.keys);algorithmResults.clear()}
  fun deleteSelected(){if(selectedPoints.isEmpty()&&selectedSegments.isEmpty())return;rememberUndo();selectedPoints.forEach{points.remove(it)};selectedSegments.forEach{segments.remove(it)};selectedPoints.clear();selectedSegments.clear();algorithmResults.clear()}
  fun executeAlgorithm(descriptor: AlgorithmDescriptor) { val selection=GeometrySelection.Points(selectedPoints.sorted().mapNotNull{points[it]?.let{p->IdentifiedPoint(it,p)}}); val segSelection=GeometrySelection.Segments(selectedSegments.sorted().mapNotNull{segments[it]?.let{ s->IdentifiedSegment(s.id,s.start,s.end)}}); if(!descriptor.isEligible(selection,segSelection))return; try { algorithmResults[descriptor.id]=descriptor.factory().execute(selection,segSelection) } catch(e:Exception){algorithmResults[descriptor.id]=AlgorithmResult.Error(e.message?:"Algorithm failed")} }
  fun executeAlgorithm(algorithm:GeometricAlgorithm){executeAlgorithm(AlgorithmDescriptor(algorithm.getName(),algorithm.getName(),AlgorithmCategory.POINTS,"",AlgorithmInput.POINTS,algorithm.getMinimumPoints(),factory={ object: GeometryAlgorithm { override fun execute(points: GeometrySelection.Points, segments: GeometrySelection.Segments)=algorithm.execute(points.points.map { it.position }) } }))}
  fun clearAlgorithmResults(){algorithmResults.clear()}
}

enum class InteractionMode { ADD_POINTS, DRAW_SEGMENTS, SELECT }
data class SegmentEntity(val id:Int,val start:Vec2,val end:Vec2)
data class IdentifiedPoint(val id:Int,val position:Vec2)
data class IdentifiedSegment(val id:Int,val start:Vec2,val end:Vec2)
sealed interface GeometrySelection { data class Points(val points:List<IdentifiedPoint>):GeometrySelection; data class Segments(val segments:List<IdentifiedSegment>):GeometrySelection }
enum class AlgorithmCategory { POINTS, SEGMENTS, POLYGONS, INTERSECTIONS, HULLS, DISTANCES }
enum class AlgorithmInput { POINTS, SEGMENTS, MIXED }
data class AlgorithmDescriptor(val id:String,val displayName:String,val category:AlgorithmCategory,val description:String,val input:AlgorithmInput,val minimumItems:Int,val tags:Set<String> = emptySet(),val factory:()->GeometryAlgorithm) { fun isEligible(points:GeometrySelection.Points,segments:GeometrySelection.Segments)=when(input){AlgorithmInput.POINTS->points.points.size>=minimumItems;AlgorithmInput.SEGMENTS->segments.segments.size>=minimumItems;AlgorithmInput.MIXED->points.points.size+segments.segments.size>=minimumItems} }

@Composable fun rememberCanvasState():CanvasState=remember{CanvasState()}
sealed class AlgorithmResult { data class PointPair(val point1:Vec2,val point2:Vec2,val distance:Double,val color:Color=Color.Red):AlgorithmResult();data class Line(val start:Vec2,val end:Vec2,val color:Color=Color.Blue):AlgorithmResult();data class Points(val points:List<Vec2>,val color:Color=Color.Green):AlgorithmResult();data class Lines(val lines:List<Line>):AlgorithmResult();enum class IntersectionKind{POINT,OVERLAP};data class SegmentOverlay(val id:Int,val start:Vec2,val end:Vec2);data class SegmentIntersectionResult(val firstSegment:Int,val secondSegment:Int,val kind:IntersectionKind,val point:Vec2?=null,val overlapStart:Vec2?=null,val overlapEnd:Vec2?=null);data class BentleyOttmannResult(val segments:List<SegmentOverlay>,val intersections:List<SegmentIntersectionResult>,val segmentColor:Color=Color(0xFF7E57C2),val pointColor:Color=Color(0xFFE65100),val overlapColor:Color=Color(0xFFD32F2F)):AlgorithmResult();data class Error(val message:String):AlgorithmResult() }
interface GeometricAlgorithm { fun getName():String;fun getMinimumPoints():Int;fun execute(points:List<Vec2>):AlgorithmResult }
interface GeometryAlgorithm { fun execute(points:GeometrySelection.Points,segments:GeometrySelection.Segments):AlgorithmResult }