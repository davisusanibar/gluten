/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include "shuffle/ShuffleWriter.h"

namespace gluten {

class ShuffleWriter::PartitionWriter {
 public:
  static arrow::Result<std::shared_ptr<ShuffleWriter::PartitionWriter>> Make(
      ShuffleWriter* shuffle_writer,
      int32_t num_partitions);

 public:
  PartitionWriter(ShuffleWriter* shuffle_writer, int32_t num_partitions)
      : shuffle_writer_(shuffle_writer), num_partitions_(num_partitions) {}
  virtual ~PartitionWriter() = default;

  virtual arrow::Status Init() = 0;

  virtual arrow::Status EvictPartition(int32_t partition_id) = 0;

  virtual arrow::Status Stop() = 0;

  ShuffleWriter* shuffle_writer_;
  uint32_t num_partitions_;
};

} // namespace gluten
