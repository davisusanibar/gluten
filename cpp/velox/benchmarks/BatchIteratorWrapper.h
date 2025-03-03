#pragma once

#include "BenchmarkUtils.h"

#include "compute/ResultIterator.h"
#include "memory/ColumnarBatch.h"
#include "memory/ColumnarBatchIterator.h"

#include <arrow/adapters/orc/adapter.h>
#include <arrow/c/bridge.h>
#include <arrow/io/api.h>
#include <arrow/record_batch.h>
#include <arrow/util/range.h>
#include <parquet/arrow/reader.h>

namespace gluten {

using GetInputFunc = std::shared_ptr<gluten::ResultIterator>(const std::string&);

class BatchIterator : public ColumnarBatchIterator {
 public:
  explicit BatchIterator(const std::string& path) : path_(path) {}

  virtual ~BatchIterator() = default;

  virtual void CreateReader() = 0;

  virtual std::shared_ptr<arrow::Schema> GetSchema() = 0;

  int64_t GetCollectBatchTime() const {
    return collectBatchTime_;
  }

 protected:
  int64_t collectBatchTime_ = 0;
  std::string path_;
};

class ParquetBatchIterator : public BatchIterator {
 public:
  explicit ParquetBatchIterator(const std::string& path) : BatchIterator(getExampleFilePath(path)) {}

  void CreateReader() override {
    parquet::ArrowReaderProperties properties = parquet::default_arrow_reader_properties();
    GLUTEN_THROW_NOT_OK(parquet::arrow::FileReader::Make(
        arrow::default_memory_pool(), parquet::ParquetFileReader::OpenFile(path_), properties, &fileReader_));
    GLUTEN_THROW_NOT_OK(
        fileReader_->GetRecordBatchReader(arrow::internal::Iota(fileReader_->num_row_groups()), &recordBatchReader_));

    auto schema = recordBatchReader_->schema();
    std::cout << "schema:\n" << schema->ToString() << std::endl;
  }

  std::shared_ptr<arrow::Schema> GetSchema() override {
    return recordBatchReader_->schema();
  }

 protected:
  std::unique_ptr<parquet::arrow::FileReader> fileReader_;
  std::shared_ptr<arrow::RecordBatchReader> recordBatchReader_;
};

class OrcBatchIterator : public BatchIterator {
 public:
  explicit OrcBatchIterator(const std::string& path) : BatchIterator(path) {}

  void CreateReader() override {
    // Open File
    auto input = arrow::io::ReadableFile::Open(path_);
    GLUTEN_THROW_NOT_OK(input);

    // Open ORC File Reader
    auto maybeReader = arrow::adapters::orc::ORCFileReader::Open(*input, arrow::default_memory_pool());
    GLUTEN_THROW_NOT_OK(maybeReader);
    fileReader_.reset((*maybeReader).release());

    // get record batch Reader
    auto recordBatchReader = fileReader_->GetRecordBatchReader(4096, std::vector<std::string>());
    GLUTEN_THROW_NOT_OK(recordBatchReader);
    recordBatchReader_ = *recordBatchReader;
  }

  std::shared_ptr<arrow::Schema> GetSchema() override {
    auto schema = fileReader_->ReadSchema();
    GLUTEN_THROW_NOT_OK(schema);
    return *schema;
  }

 protected:
  std::unique_ptr<arrow::adapters::orc::ORCFileReader> fileReader_;
  std::shared_ptr<arrow::RecordBatchReader> recordBatchReader_;
};

} // namespace gluten
