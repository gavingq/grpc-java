/*
 * Copyright 2017, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.testing.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * This servlet communicates with {@code grpc-test.sandbox.googleapis.com}, which is a server
 * managed by the gRPC team. For more information, see
 * <a href="https://github.com/grpc/grpc/blob/master/doc/interop-test-descriptions.md">
 *   Interoperability Test Case Descriptions</a>.
 */
@SuppressWarnings("serial")
public final class NettyClientInteropServlet extends HttpServlet {
  private static final String INTEROP_TEST_ADDRESS = "grpc-test.sandbox.googleapis.com:443";

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    PrintWriter writer = resp.getWriter();
    writer.println("Test invoked at: ");
    writer.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z")
        .format(Calendar.getInstance().getTime()));

    Result result = new JUnitCore().run(Tester.class);
    if (result.wasSuccessful()) {
      resp.setStatus(200);
      writer.println(
          String.format(
              "PASS! Tests ran %d, tests ignored %d",
              result.getRunCount(),
              result.getIgnoreCount()));
    } else {
      resp.setStatus(500);
      writer.println(
          String.format(
              "FAILED! Tests ran %d, tests failed %d, tests ignored %d",
              result.getRunCount(),
              result.getFailureCount(),
              result.getIgnoreCount()));
      for (Failure failure : result.getFailures()) {
        writer.println("================================");
        writer.println(failure.getTestHeader());
        Throwable thrown = failure.getException();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        thrown.printStackTrace(printWriter);
        writer.println(stringWriter);
      }
    }
  }

  public static final class Tester extends AbstractInteropTest {
    @Override
    protected ManagedChannel createChannel() {
      assertEquals(
          "jdk8 required",
          "1.8",
          System.getProperty("java.specification.version"));
      ManagedChannelBuilder<?> builder =
          ManagedChannelBuilder.forTarget(INTEROP_TEST_ADDRESS)
              .maxInboundMessageSize(AbstractInteropTest.MAX_MESSAGE_SIZE);
      assertTrue(builder instanceof NettyChannelBuilder);
      ((NettyChannelBuilder) builder).flowControlWindow(65 * 1024);
      return builder.build();
    }

    @Override
    protected boolean metricsExpected() {
      // Server-side metrics won't be found because the server is running remotely.
      return false;
    }

    // grpc-test.sandbox.googleapis.com does not support these tests
    @Override
    public void customMetadata() { }

    @Override
    public void statusCodeAndMessage() { }

    @Override
    public void exchangeMetadataUnaryCall() { }

    @Override
    public void exchangeMetadataStreamingCall() { }
  }
}
