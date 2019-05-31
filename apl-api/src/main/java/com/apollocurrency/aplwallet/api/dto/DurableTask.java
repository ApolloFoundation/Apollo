/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Representation of task that takes noticeable time
 * and should de displayed to user in UI
 * @author alukin@gmail.com
 */
public class DurableTask {
    String id;
    Date started;
    Date finished;
    Double percentComplete;
    String decription;
    String stateOfTask;
    List<String> messages=new ArrayList<>();
}
