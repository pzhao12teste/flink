/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.descriptors

import org.apache.flink.table.api.ValidationException
import org.apache.flink.table.descriptors.StatisticsValidator.{STATISTICS_COLUMNS, STATISTICS_ROW_COUNT, readColumnStats}
import org.apache.flink.table.plan.stats.TableStats

import scala.collection.JavaConverters._

/**
  * Common class for all descriptors describing a table source.
  */
abstract class TableSourceDescriptor(connector: ConnectorDescriptor) extends Descriptor {

  protected val connectorDescriptor: ConnectorDescriptor = connector

  protected var formatDescriptor: Option[FormatDescriptor] = None
  protected var schemaDescriptor: Option[Schema] = None
  protected var statisticsDescriptor: Option[Statistics] = None
  protected var metaDescriptor: Option[Metadata] = None

  /**
    * Internal method for properties conversion.
    */
  override private[flink] def addProperties(properties: DescriptorProperties): Unit = {
    connectorDescriptor.addProperties(properties)

    // check for a format
    if (connectorDescriptor.needsFormat() && formatDescriptor.isEmpty) {
      throw new ValidationException(
        s"The connector '$connectorDescriptor' requires a format description.")
    } else if (!connectorDescriptor.needsFormat() && formatDescriptor.isDefined) {
      throw new ValidationException(
        s"The connector '$connectorDescriptor' does not require a format description " +
          s"but '${formatDescriptor.get}' found.")
    }

    formatDescriptor.foreach(_.addProperties(properties))
    schemaDescriptor.foreach(_.addProperties(properties))
    metaDescriptor.foreach(_.addProperties(properties))
  }

  /**
    * Reads table statistics from the descriptors properties.
    */
  protected def getTableStats: Option[TableStats] = {
      val normalizedProps = new DescriptorProperties()
      addProperties(normalizedProps)
      val rowCount = normalizedProps.getLong(STATISTICS_ROW_COUNT).map(v => Long.box(v))
      rowCount match {
        case Some(cnt) =>
          val columnStats = readColumnStats(normalizedProps, STATISTICS_COLUMNS)
          Some(TableStats(cnt, columnStats.asJava))
        case None =>
          None
      }
    }
}
