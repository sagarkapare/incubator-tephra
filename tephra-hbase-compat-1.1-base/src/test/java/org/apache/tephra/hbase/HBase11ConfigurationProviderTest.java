/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tephra.hbase;

import com.google.common.collect.ImmutableList;
import org.apache.tephra.util.AbstractConfigurationProviderTest;
import org.apache.tephra.util.HBaseVersion;

import java.util.Collection;

/**
 * Test for HBase 1.1 and HBase 1.2 versions specific behavior.
 */
public class HBase11ConfigurationProviderTest extends AbstractConfigurationProviderTest {
  @Override
  protected Collection<HBaseVersion.Version> getExpectedVersions() {
    return ImmutableList.of(HBaseVersion.Version.HBASE_11, HBaseVersion.Version.HBASE_12);
  }
}
