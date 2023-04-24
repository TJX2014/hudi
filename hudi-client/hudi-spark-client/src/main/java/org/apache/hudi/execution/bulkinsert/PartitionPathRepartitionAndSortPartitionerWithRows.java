/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hudi.execution.bulkinsert;

import org.apache.hudi.common.model.HoodieRecord;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * A built-in partitioner that does the following for input rows for bulk insert operation
 * <p>
 * - For physically partitioned table, repartition the input rows based on the partition path,
 * and sort rows within Spark partitions, limiting the shuffle parallelism to specified
 * `outputSparkPartitions`
 * <p>
 * - For physically non-partitioned table, simply does coalesce for the input rows with
 * `outputSparkPartitions`
 * <p>
 * Corresponding to the {@code BulkInsertSortMode.PARTITION_PATH_REPARTITION_AND_SORT} mode.
 */
public class PartitionPathRepartitionAndSortPartitionerWithRows extends TargetGroupAssignedBulkInsertPartitioner<Dataset<Row>> {

  private final boolean isTablePartitioned;

  public PartitionPathRepartitionAndSortPartitionerWithRows(boolean isTablePartitioned, String targetFileGroupId) {
    super(targetFileGroupId);
    this.isTablePartitioned = isTablePartitioned;
  }

  @Override
  public Dataset<Row> repartitionRecords(Dataset<Row> rows, int outputSparkPartitions) {
    if (isTablePartitioned) {
      return rows.repartition(outputSparkPartitions, new Column(HoodieRecord.PARTITION_PATH_METADATA_FIELD))
          .sortWithinPartitions(new Column(HoodieRecord.PARTITION_PATH_METADATA_FIELD));
    }
    return rows.coalesce(outputSparkPartitions);
  }

  @Override
  public boolean arePartitionRecordsSorted() {
    return isTablePartitioned;
  }
}
