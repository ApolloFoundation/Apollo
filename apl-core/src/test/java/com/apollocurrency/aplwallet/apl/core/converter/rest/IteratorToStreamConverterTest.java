/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.rest;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IteratorToStreamConverterTest {

    IteratorToStreamConverter<Object> converter = new IteratorToStreamConverter<>();
    DbIterator iterator = mock(DbIterator.class);
    AtomicInteger hasNextCounter = new AtomicInteger();
    AtomicInteger nextCounter = new AtomicInteger();
    Stream<Integer> stream = converter.apply(iterator).skip(1).limit(1);
    @Test
    void iteratorToStream_notClosed() {
        Iterator<Integer> iterator = List.of(1, 2, 3).iterator();
        Spliterator<Integer> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        Stream<Integer> stream = StreamSupport.stream(spliterator, false);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        stream.onClose(()-> streamClosed.set(true));

        List<Integer> limit = stream.skip(1).limit(1).collect(Collectors.toList());

        assertEquals(List.of(2), limit);
        assertTrue(iterator.hasNext());
        assertEquals(iterator.next(), 3);
        assertFalse(streamClosed.get());
    }

    @BeforeEach
    public void setUp() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (hasNextCounter.incrementAndGet() <= 3) {
                    return true;
                }
                return false;
            }
        }).when(iterator).hasNext();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (nextCounter.incrementAndGet() <= 3) {
                    return nextCounter.get() + 10;
                }
                return null;
            }
        }).when(iterator).next();
    }
    @Test
    void testApply() {

        List<Integer> collect = stream.collect(Collectors.toList());

        stream.close();
        verifyResults(collect);
    }

    private void verifyResults(List<Integer> collect) {
        assertEquals(List.of(12), collect);
        doCounterVerification();
    }

    private void doCounterVerification() {
        assertEquals(2, nextCounter.get());
        assertEquals(2, hasNextCounter.get());
        verify(iterator).close();
    }

    @Test
    void testForEach() {
        List<Integer> collect = new ArrayList<>();
            CollectionUtil.forEach(stream, collect::add);

        verifyResults(collect);
    }

    @Test
    void testToList() {
        List<Integer> list = CollectionUtil.toList(stream);

        verifyResults(list);
    }

    @Test
    void testCount() {
        long count = CollectionUtil.count(stream);

        assertEquals(1, count);
        doCounterVerification();
    }

    @Test
    void testIteratorToList() {
        List<Integer> list = CollectionUtil.toList(iterator);
        assertEquals(List.of(11,12,13), list);
    }
}