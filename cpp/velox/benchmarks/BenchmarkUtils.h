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

#include <benchmark/benchmark.h>

#include <velox/common/memory/Memory.h>

#include <velox/dwio/common/tests/utils/DataFiles.h>
#include <velox/substrait/SubstraitToVeloxPlan.h>
#include <cstdlib>
#include <filesystem>
#include <fstream>

#include <thread>
#include <utility>

#include "compute/ProtobufUtils.h"
#include "velox/common/memory/Memory.h"

DECLARE_bool(print_result);
DECLARE_string(write_file);
DECLARE_string(batch_size);
DECLARE_int32(cpu);
DECLARE_int32(threads);
DECLARE_int32(iterations);

/// Initilize the Velox backend with default value.
void InitVeloxBackend();

/// Initilize the Velox backend.
void InitVeloxBackend(std::unordered_map<std::string, std::string>& conf);

/// Get the location of a file in this project.
inline std::string getExampleFilePath(const std::string& fileName) {
  std::filesystem::path path(fileName);
  if (path.is_absolute()) {
    return fileName;
  }
  return facebook::velox::test::getDataFilePath("cpp/velox/benchmarks", "data/" + fileName);
}

// Get the location of a file generated by Java unittest.
inline arrow::Result<std::string> getGeneratedFilePath(const std::string& fileName) {
  std::string currentPath = std::filesystem::current_path().c_str();
  auto generatedFilePath = currentPath + "/../../../../backends-velox/generated-native-benchmark/";
  std::filesystem::directory_entry filePath{generatedFilePath + fileName};
  if (filePath.exists()) {
    if (filePath.is_regular_file() && filePath.path().extension().native() == ".json") {
      // If fileName points to a regular file, it should be substrait json plan.
      return filePath.path().c_str();
    } else if (filePath.is_directory()) {
      // If fileName points to a directory, get the generated parquet data.
      auto dirItr = std::filesystem::directory_iterator(std::filesystem::path(filePath));
      for (auto& itr : dirItr) {
        if (itr.is_regular_file() && itr.path().extension().native() == ".parquet") {
          return itr.path().c_str();
        }
      }
    }
  }
  return arrow::Status::Invalid("Could not get generated file from given path: " + fileName);
}

/// Read binary data from a json file.
arrow::Result<std::shared_ptr<arrow::Buffer>> getPlanFromFile(const std::string& filePath);

/// Get the file paths, starts, lengths from a directory.
/// Use fileFormat to specify the format to read, eg., orc, parquet.
/// Return a split info.
std::shared_ptr<facebook::velox::substrait::SplitInfo> getSplitInfos(
    const std::string& datasetPath,
    const std::string& fileFormat);

bool CheckPathExists(const std::string& filepath);

void AbortIfFileNotExists(const std::string& filepath);

/// Return whether the data ends with suffix.
bool EndsWith(const std::string& data, const std::string& suffix);

void setCpu(uint32_t cpuindex);

std::shared_ptr<arrow::Schema> getOutputSchema(std::shared_ptr<const facebook::velox::core::PlanNode> plan);
