/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools;

import com.beust.jcommander.IStringConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IntegerListConverter implements IStringConverter<List<Integer>> {
    @Override
    public List<Integer> convert(String files) {
        String [] paths = files.split(",");
        return Arrays.stream(paths).map(Integer::parseInt).collect(Collectors.toList());
    }
}
