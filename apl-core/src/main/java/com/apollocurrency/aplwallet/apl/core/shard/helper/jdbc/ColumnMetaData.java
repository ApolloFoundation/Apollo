/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc;

import java.util.Objects;

/**
 * Class keeps sql column meta data for CSV export/import
 * Header can be written in format = COLUMN_NAME(TYPE|PRECISION|SCALE)
 *
 * @author yuriy.larin
 */
public class ColumnMetaData {
    /**
     * The column label.
     */
    private String name;

    /**
     * The column type Name
     */
    private String sqlTypeName;

    /**
     * The SQL type as int.
     */
    private int sqlTypeInt;

    /**
     * The precision.
     */
    private int precision;

    /**
     * The scale.
     */
    private int scale;
    // in case we'll want to use diffrent separators
    private char fieldTypeSeparatorStart = '(';
    private char fieldTypeSeparatorEnd = ')';

    public ColumnMetaData() {
    }

    public ColumnMetaData(String name, String sqlTypeName, int sqlType, int precision, int scale) {
        this.name = name;
        this.sqlTypeName = sqlTypeName;
        this.sqlTypeInt = sqlType;
        this.precision = precision;
        this.scale = scale;
    }

    public ColumnMetaData(String name, String sqlTypeName, int sqlType, int precision, int scale,
                          char fieldTypeSeparatorStart, char fieldTypeSeparatorEnd) {
        this.name = name;
        this.sqlTypeName = sqlTypeName;
        this.sqlTypeInt = sqlType;
        this.precision = precision;
        this.scale = scale;
        this.fieldTypeSeparatorStart = fieldTypeSeparatorStart;
        this.fieldTypeSeparatorEnd = fieldTypeSeparatorEnd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSqlTypeName() {
        return sqlTypeName;
    }

    public void setSqlTypeName(String sqlTypeName) {
        this.sqlTypeName = sqlTypeName;
    }

    public int getSqlTypeInt() {
        return sqlTypeInt;
    }

    public void setSqlTypeInt(int sqlTypeInt) {
        this.sqlTypeInt = sqlTypeInt;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnMetaData that = (ColumnMetaData) o;
        return sqlTypeInt == that.sqlTypeInt &&
            precision == that.precision &&
            scale == that.scale &&
            Objects.equals(name, that.name) &&
            Objects.equals(sqlTypeName, that.sqlTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sqlTypeName, sqlTypeInt, precision, scale);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(name).append(this.fieldTypeSeparatorStart);
        sb.append(sqlTypeInt);
        sb.append("|").append(precision);
        sb.append("|").append(scale);
        sb.append(this.fieldTypeSeparatorEnd);
        return sb.toString();
    }
}
