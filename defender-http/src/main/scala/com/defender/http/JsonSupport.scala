package com.defender.http

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.defender.logging.LogEvent
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
    private val iso_date_time = DateTimeFormatter.ISO_DATE_TIME
    def write(x: LocalDateTime) = JsString(iso_date_time.format(x))
    def read(value: JsValue): LocalDateTime = value match {
      case JsString(x) => LocalDateTime.parse(x, iso_date_time)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }

  implicit val logEventFormat: RootJsonFormat[LogEvent] = jsonFormat4(LogEvent)
}

