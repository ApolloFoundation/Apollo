/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdQueueTest {
    IdQueue<Entity> idQueue;

    Entity first = new Entity(1, "entity 1");
    Entity second = new Entity(2, "entity 2");

    private void initQueue() {
       idQueue = new IdQueue<>(new ArrayDeque<>(), Entity::getId, 2);
    }

    @Test
    void testAddRemove() {
        initQueue();

        addSuccessfully(first);
        addFailedAlreadyContains(new Entity(first.id, ""));
        addSuccessfully(second);
        addFailedAlreadyContains(second);
        addFailedFull(new Entity(3, "entity3"));

        assertEquals(2, idQueue.size());
        Entity remove1 = idQueue.remove();
        assertEquals(remove1, first);
        Entity remove2 = idQueue.remove();
        assertEquals(remove2, second);
    }

    @Test
    void testAddWithStatus() {
        initQueue();

        addSuccessfully(first);
        IdQueue.ReturnCode returnCode = idQueue.addWithStatus(second);
        assertEquals(IdQueue.ReturnCode.ADDED, returnCode);
        assertTrue(returnCode.isOk());
        IdQueue.ReturnCode fullCode = idQueue.addWithStatus(second);
        assertEquals(IdQueue.ReturnCode.FULL, fullCode);
        Entity remove = idQueue.remove();
        assertEquals(first, remove);
        IdQueue.ReturnCode alreadyExist = idQueue.addWithStatus(second);
        assertEquals(IdQueue.ReturnCode.ALREADY_EXIST, alreadyExist);
    }

    @Test
    void testClear() {
        initQueue();

        addSuccessfully(first);
        addSuccessfully(second);

        idQueue.clear();
        assertEquals(0, idQueue.size());
        notContains(first);
        notContains(second);

    }

    @Test
    void testRemoveObj() {
        initQueue();
        addSuccessfully(first);
        addSuccessfully(second);

        idQueue.remove(first);
        assertEquals(1, idQueue.size());
        notContains(first);
        contains(second);

    }

    @Test
    void testIteratorRemove() {
        idQueue = new IdQueue<>(new ArrayDeque<>(), Entity::getId,2);
        addSuccessfully(first);
        addSuccessfully(second);

        Iterator<Entity> iterator = idQueue.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(0, idQueue.size());
        notContains(first);
        notContains(second);
    }

    private void addFailedFull(Entity entity) {
        int size = idQueue.size();
        boolean add = idQueue.add(entity);
        assertFalse(add, "Queue is expected to be full, but got " + size + ", queue " + idQueue);
        boolean contains = idQueue.contains(entity);
        assertFalse(contains, "Expected no " + entity + " to be in the queue " + idQueue);
        notContains(entity);
        assertEquals(size, idQueue.size(), "Size should not be changed during failed 'add' call");
    }

    void addSuccessfully(Entity entity) {
        int size = idQueue.size();
        boolean add = idQueue.add(entity);
        assertTrue(add, "Expected " + entity + " in the queue " + idQueue);
        boolean contains = idQueue.contains(entity);
        assertTrue(contains, "Expected " + entity + " to be in the queue " + idQueue);
        assertEquals(size + 1, idQueue.size(), "Size be changed (+1) during add call");

    }
    void addFailed(Entity entity) {
        int size = idQueue.size();
        boolean add = idQueue.add(entity);
        assertFalse(add, "Expected no " + entity + " in the queue " + idQueue);
        notContains(entity);
        assertEquals(size, idQueue.size(), "Size should not be changed during failed add call");
    }

    void addFailedAlreadyContains(Entity entity) {
        int size = idQueue.size();
        boolean add = idQueue.add(entity);
        assertFalse(add, "Expected already exists " + entity + " in the queue " + idQueue);
        assertEquals(size, idQueue.size(), "Size should not be changed during failed add call");
    }

    void notContains(Entity entity) {
        boolean contains = idQueue.contains(entity);
        assertFalse(contains, "Expected no " + entity + " to be in the queue " + idQueue);
    }
    void contains(Entity entity) {
        boolean contains = idQueue.contains(entity);
        assertTrue(contains, "Expected " + entity + " to be in the queue " + idQueue);
    }


    @Data
    @AllArgsConstructor
    private static class Entity {
        private long id;
        private String data;
    }

}