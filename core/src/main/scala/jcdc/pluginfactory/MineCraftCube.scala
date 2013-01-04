package jcdc.pluginfactory

import org.bukkit.block.Block
import org.bukkit.entity.Player
import BukkitEnrichment._
import org.bukkit.{Material, Location}
import collection.JavaConversions.asScalaIterator

object MineCraftCube {
  implicit class LocationToCoor(l: Location){
    def coor = Coor(l.xd, l.yd, l.zd)
  }
  implicit class BlockToCoor(b: Block){
    def coor = Coor(b.xd, b.yd, b.zd)
  }
  def apply(b1: Block, b2: Block): MineCraftCube = new MineCraftCube(b1.loc, b2.loc)

  type BandM = (Block, MaterialAndData)
  type Changes = Vector[Change]

  type Change = (Block, MaterialAndData)

  object PotentialChange {
    def fromChange(c: Change) = new PotentialChange(c._1, c._2)
  }

  case class PotentialChange(b: Block, newM: MaterialAndData){
    val oldM = b.materialAndData
    def run: Boolean = newM update b
  }

  object Changer {
    def forall(bms: Stream[Block], newM: MaterialAndData): Stream[PotentialChange] =
      bms.zip(Stream.continually(newM)).map{ case (b,n) => PotentialChange(b,n) }

    def runAll(bms: Stream[Block], newM: MaterialAndData) = runChanges(forall(bms, newM))

    def runChanges(newData: Stream[PotentialChange]): Changes =
      newData.filter(_.run).map(p => (p.b, p.oldM)).toVector
  }
}

case class MineCraftCube(loc1: Location, loc2: Location) extends Cube[Block] {

  import MineCraftCube._

  val corner1 = loc1.coor
  val corner2 = loc2.coor
  val f = (c: Coor) => loc1.world(c.xd, c.yd, c.zd)

  val world = loc1.world
  def players: Iterator[Player] = world.getPlayers.iterator.filter(contains)

  def blocks = toStream
  def blocksAndMaterials = blocks.map(b => (b, b.materialAndData))

  def contains(p: Player)  : Boolean = this.contains(p.loc.coor)
  def contains(l: Location): Boolean = this.contains(l.coor)

  /**
   * Set all the the blocks in this cube to the new type.
   * @param newM
   */
  def setAll(newM: Material): Changes = setAll(new MaterialAndData(newM, None))

  /**
   * Set all the the blocks in this cube to the new type.
   * @param newM
   */
  def setAll(newM: MaterialAndData): Changes = Changer.runAll(blocks, newM)

  /**
   * Change all of the blocks in this cube that are of the old material type
   * to the new material type.
   * @param oldM
   * @param newM
   */
  def changeAll(oldM: Material, newM: Material): Changes =
    changeAll(oldM, new MaterialAndData(newM, None))

  /**
   * Change all of the blocks in this cube that are of the old material type
   * to the new material type.
   * @param oldM
   * @param newM
   */
  def changeAll(oldM: Material, newM: MaterialAndData): Changes =
    Changer.runAll(blocks.filter(_ is oldM), newM)

  /**
   * Set all the blocks in this Cube to AIR
   */
  def eraseAll: Int = toStream.count(_.erase)

  /**
   * this is pretty close to map, on a Cube...
   * @param newL1
   */
  def paste(newL1: Location): Changes = {
    def translate(b: Block): Block = world(
      b.xd + (newL1.xd - b.xd) + (b.xd - minXd),
      b.yd + (newL1.yd - b.yd) + (b.yd - minYd),
      b.zd + (newL1.zd - b.zd) + (b.zd - minZd)
    )
    Changer.runChanges(blocks.map(b => PotentialChange(translate(b), b.materialAndData)))
  }
}