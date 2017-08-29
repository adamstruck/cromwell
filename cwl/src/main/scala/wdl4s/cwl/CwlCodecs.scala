package wdl4s.cwl

import io.circe._
import io.circe.parser._
import io.circe.shapes._
import io.circe.generic.auto._
import eu.timepit.refined.string._
import io.circe.refined._
import io.circe.literal._

object CwlCodecs {
  import Implicits._

  //According to automatic derivation, these instances should not be required.  But
  //removing these breaks decodeCwl, so...
  implicit val wfD = implicitly[Decoder[Workflow]]
  implicit val cltD = implicitly[Decoder[CommandLineTool]]

  def decodeCwl(in: String) = decode[Cwl](in)

  def encodeCwlCommandLineTool(commandLineTool: CommandLineTool): Json = {
    import io.circe.syntax._
    import wdl4s.cwl.Implicits.enumerationEncoder
    commandLineTool.asJson
  }

  def encodeCwlWorkflow(workflow: Workflow): Json = {
    import io.circe.syntax._
    import wdl4s.cwl.Implicits.enumerationEncoder
    workflow.asJson
  }

  def encodeCwl(cwl: Cwl): Json = cwl.fold(CwlEncoder)

  val jsonPrettyPrinter = io.circe.Printer.spaces2.copy(dropNullKeys = true, preserveOrder = true)

  val yamlPrettyPrinter = io.circe.yaml.Printer.spaces2.copy(dropNullKeys = true, preserveOrder = true)

  def cwlToJson(cwl: Cwl): String = jsonPrettyPrinter.pretty(encodeCwl(cwl))

  def cwlToYaml(cwl: Cwl): String = yamlPrettyPrinter.pretty(encodeCwl(cwl))

}
