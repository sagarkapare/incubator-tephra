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

package org.apache.tephra.hbase.coprocessor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterBase;
import org.apache.hadoop.hbase.regionserver.ScanType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.tephra.Transaction;
import org.apache.tephra.TxConstants;
import org.apache.tephra.hbase.AbstractTransactionVisibilityFilterTest;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * HBase 1.1 specific test for filtering logic applied when reading data transactionally.
 */
public class TransactionVisibilityFilterTest extends AbstractTransactionVisibilityFilterTest {
  /**
   * Test filtering of KeyValues for in-progress and invalid transactions.
   * @throws Exception
   */
  @Test
  public void testFiltering() throws Exception {
    TxFilterFactory txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new TransactionVisibilityFilter(tx, familyTTLs, false, ScanType.USER_SCAN);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL));
  }

  @Test
  public void testSubFilter() throws Exception {
    final FilterBase includeFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.INCLUDE;
      }
    };
    TxFilterFactory txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new TransactionVisibilityFilter(tx, familyTTLs, false, ScanType.USER_SCAN, includeFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL));

    final Filter skipFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.SKIP;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new TransactionVisibilityFilter(tx, familyTTLs, false, ScanType.USER_SCAN, skipFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL));

    final Filter includeNextFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.INCLUDE_AND_NEXT_COL;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new TransactionVisibilityFilter(tx, familyTTLs, false, ScanType.USER_SCAN, includeNextFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL));

    final Filter nextColFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.NEXT_COL;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new TransactionVisibilityFilter(tx, familyTTLs, false, ScanType.USER_SCAN, nextColFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL));

  }

  @Test
  public void testSubFilterOverride() throws Exception {
    final FilterBase includeFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.INCLUDE;
      }
    };
    TxFilterFactory txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new CustomTxFilter(tx, familyTTLs, false, ScanType.USER_SCAN, includeFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.INCLUDE,
                                      Filter.ReturnCode.INCLUDE,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.INCLUDE,
                                      Filter.ReturnCode.INCLUDE));

    final Filter skipFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.SKIP;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new CustomTxFilter(tx, familyTTLs, false, ScanType.USER_SCAN, skipFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL));

    final Filter includeNextFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.INCLUDE_AND_NEXT_COL;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new CustomTxFilter(tx, familyTTLs, false, ScanType.USER_SCAN, includeNextFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                                      Filter.ReturnCode.INCLUDE_AND_NEXT_COL));

    final Filter nextColFilter = new FilterBase() {
      @Override
      public ReturnCode filterKeyValue(Cell ignored) throws IOException {
        return ReturnCode.NEXT_COL;
      }
    };
    txFilterFactory = new TxFilterFactory() {
      @Override
      public Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs) {
        return new CustomTxFilter(tx, familyTTLs, false, ScanType.USER_SCAN, nextColFilter);
      }
    };
    runFilteringTest(txFilterFactory,
                     ImmutableList.of(Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.SKIP,
                                      Filter.ReturnCode.NEXT_COL,
                                      Filter.ReturnCode.NEXT_COL));

  }

  private void runFilteringTest(TxFilterFactory txFilterFactory,
                                List<Filter.ReturnCode> assertCodes) throws Exception {

    /*
     * Start and stop some transactions.  This will give us a transaction state something like the following
     * (numbers only reflect ordering, not actual transaction IDs):
     *   6  - in progress
     *   5  - committed
     *   4  - invalid
     *   3  - in-progress
     *   2  - committed
     *   1  - committed
     *
     *   read ptr = 5
     *   write ptr = 6
     */

    Transaction tx1 = txManager.startShort();
    assertTrue(txManager.canCommit(tx1, EMPTY_CHANGESET));
    assertTrue(txManager.commit(tx1));

    Transaction tx2 = txManager.startShort();
    assertTrue(txManager.canCommit(tx2, EMPTY_CHANGESET));
    assertTrue(txManager.commit(tx2));

    Transaction tx3 = txManager.startShort();
    Transaction tx4 = txManager.startShort();
    txManager.invalidate(tx4.getTransactionId());

    Transaction tx5 = txManager.startShort();
    assertTrue(txManager.canCommit(tx5, EMPTY_CHANGESET));
    assertTrue(txManager.commit(tx5));

    Transaction tx6 = txManager.startShort();

    Map<byte[], Long> ttls = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
    Filter filter = txFilterFactory.getTxFilter(tx6, ttls);

    assertEquals(assertCodes.get(5),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx6.getTransactionId())));
    assertEquals(assertCodes.get(4),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx5.getTransactionId())));
    assertEquals(assertCodes.get(3),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx4.getTransactionId())));
    assertEquals(assertCodes.get(2),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx3.getTransactionId())));
    assertEquals(assertCodes.get(1),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx2.getTransactionId())));
    assertEquals(assertCodes.get(0),
                 filter.filterKeyValue(newKeyValue("row1", "val1", tx1.getTransactionId())));
  }

  /**
   * Test filtering for TTL settings.
   * @throws Exception
   */
  @Test
  public void testTTLFiltering() throws Exception {
    Map<byte[], Long> ttls = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
    ttls.put(FAM, 10L);
    ttls.put(FAM2, 30L);
    ttls.put(FAM3, 0L);

    Transaction tx = txManager.startShort();
    long now = tx.getVisibilityUpperBound();
    Filter filter = new TransactionVisibilityFilter(tx, ttls, false, ScanType.USER_SCAN);
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", now)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", now - 1 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", now - 11 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM2, "val1", now - 11 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM2, "val1", now - 21 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM2, "val1", now - 31 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", now - 31 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", now - 1001 * TxConstants.MAX_TX_PER_MS)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row2", FAM, "val1", now)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row2", FAM, "val1", now - 1 * TxConstants.MAX_TX_PER_MS)));

    // Verify ttl for pre-existing, non-transactional data
    long preNow = now / TxConstants.MAX_TX_PER_MS;
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", preNow)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", preNow - 9L)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", preNow - 10L)));
    assertEquals(Filter.ReturnCode.NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM, "val1", preNow - 11L)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", preNow)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", preNow - 9L)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", preNow - 10L)));
    assertEquals(Filter.ReturnCode.INCLUDE_AND_NEXT_COL,
                 filter.filterKeyValue(newKeyValue("row1", FAM3, "val1", preNow - 1001L)));
  }

  protected KeyValue newKeyValue(String rowkey, String value, long timestamp) {
    return new KeyValue(Bytes.toBytes(rowkey), FAM, COL, timestamp, Bytes.toBytes(value));
  }

  protected KeyValue newKeyValue(String rowkey, byte[] family, String value, long timestamp) {
    return new KeyValue(Bytes.toBytes(rowkey), family, COL, timestamp, Bytes.toBytes(value));
  }

  private interface TxFilterFactory {
    Filter getTxFilter(Transaction tx, Map<byte[], Long> familyTTLs);
  }

  private class CustomTxFilter extends TransactionVisibilityFilter {
    public CustomTxFilter(Transaction tx, Map<byte[], Long> ttlByFamily, boolean allowEmptyValues, ScanType scanType,
                          @Nullable Filter cellFilter) {
      super(tx, ttlByFamily, allowEmptyValues, scanType, cellFilter);
    }

    @Override
    protected ReturnCode determineReturnCode(ReturnCode txFilterCode, ReturnCode subFilterCode) {
      switch (subFilterCode) {
        case INCLUDE:
          return ReturnCode.INCLUDE;
        case INCLUDE_AND_NEXT_COL:
          return ReturnCode.INCLUDE_AND_NEXT_COL;
        case SKIP:
          return txFilterCode == ReturnCode.INCLUDE ? ReturnCode.SKIP : ReturnCode.NEXT_COL;
        default:
          return subFilterCode;
      }
    }
  }
}
