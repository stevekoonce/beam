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
package org.apache.beam.runners.spark.structuredstreaming.translation.helpers;

import static scala.collection.JavaConversions.asScalaBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.util.WindowedValue;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.InternalRow;

/** Helper functions for working with {@link Row}. */
public final class RowHelpers {

  /**
   * A Spark {@link MapFunction} for extracting a {@link WindowedValue} from a Row in which the
   * {@link WindowedValue} was serialized to bytes using its {@link
   * WindowedValue.WindowedValueCoder}.
   *
   * @param <T> The type of the object.
   * @return A {@link MapFunction} that accepts a {@link Row} and returns its {@link WindowedValue}.
   */
  public static <T> MapFunction<Row, WindowedValue<T>> extractWindowedValueFromRowMapFunction(
      WindowedValue.WindowedValueCoder<T> windowedValueCoder) {
    return (MapFunction<Row, WindowedValue<T>>)
        value -> {
          // there is only one value put in each Row by the InputPartitionReader
          byte[] bytes = (byte[]) value.get(0);
          return windowedValueCoder.decode(new ByteArrayInputStream(bytes));
        };
  }

  /**
   * Serialize a windowedValue to bytes using windowedValueCoder {@link
   * WindowedValue.FullWindowedValueCoder} and stores it an InternalRow.
   */
  public static <T> InternalRow storeWindowedValueInRow(
      WindowedValue<T> windowedValue, Coder<T> coder) {
    List<Object> list = new ArrayList<>();
    // serialize the windowedValue to bytes array to comply with dataset binary schema
    WindowedValue.FullWindowedValueCoder<T> windowedValueCoder =
        WindowedValue.FullWindowedValueCoder.of(coder, GlobalWindow.Coder.INSTANCE);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      windowedValueCoder.encode(windowedValue, byteArrayOutputStream);
      byte[] bytes = byteArrayOutputStream.toByteArray();
      list.add(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return InternalRow.apply(asScalaBuffer(list).toList());
  }
}
