package de.metas.ui.web.vaadin.window.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.ui.web.vaadin.window.GridRowId;
import de.metas.ui.web.vaadin.window.PropertyDescriptor;
import de.metas.ui.web.vaadin.window.PropertyName;

/*
 * #%L
 * de.metas.ui.web.vaadin
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class GridPropertyValue extends ObjectPropertyValue
{
	public static final GridPropertyValue cast(final PropertyValue propertyValue)
	{
		return (GridPropertyValue)propertyValue;
	}
	
	// private static final Logger logger = LogManager.getLogger(GridPropertyValue.class);

	private final Collection<PropertyDescriptor> columnDescriptors;

	private final LinkedHashMap<GridRowId, GridRow> rows = new LinkedHashMap<>();

	public GridPropertyValue(final PropertyValueBuilder builder)
	{
		super(builder);

		this.columnDescriptors = builder.getPropertyDescriptor().getChildPropertyDescriptors();
	}

	public GridRow newRow()
	{
		final PropertyValuesDTO values = PropertyValuesDTO.of();
		return newRow(values);
	}

	public GridRow newRow(final PropertyValuesDTO values)
	{
		final GridRow row = GridRow.of(columnDescriptors, values);
		rows.put(row.getRowId(), row);
		return row;
	}

	public int getRowsCount()
	{
		return rows.size();
	}

	private GridRow getRow(final Object rowIdObj)
	{
		final GridRowId rowId = GridRowId.of(rowIdObj);
		final GridRow row = rows.get(rowId);
		return MoreObjects.firstNonNull(row, GridRow.NULL);
	}

	public PropertyValuesDTO getRowValues(final Object rowIdObj)
	{
		return getRow(rowIdObj).getValuesAsMap();
	}

	@Override
	public Map<PropertyName, PropertyValue> getChildPropertyValues()
	{
		return ImmutableMap.of();
	}

	@Override
	public void setValue(final Object value)
	{
		final List<PropertyValuesDTO> rowValuesList;
		if (value == null)
		{
			rowValuesList = ImmutableList.of();
		}
		else if (value instanceof Supplier<?>)
		{
			@SuppressWarnings("unchecked")
			final Supplier<List<PropertyValuesDTO>> supplier = (Supplier<List<PropertyValuesDTO>>)value;
			rowValuesList = supplier.get();
		}
		else
		{
			rowValuesList = (List<PropertyValuesDTO>)value;
		}

		//
		//
		rows.clear();
		for (final PropertyValuesDTO rowValues : rowValuesList)
		{
			final GridRow row = GridRow.of(columnDescriptors, rowValues);
			final GridRowId rowId = row.getRowId();
			final GridRow rowOld = rows.put(rowId, row);
			if (rowOld != null)
			{
				// shall not happen
				throw new RuntimeException("Rows with same id '" + rowId + "' resulted from data: " + value);
			}
		}
	}

	@Override
	public Object getValue()
	{
		final List<PropertyValuesDTO> rowValuesList = new ArrayList<>(rows.size());
		for (final GridRow row : rows.values())
		{
			final PropertyValuesDTO rowValues = row.getValuesAsMap();
			rowValuesList.add(rowValues);
		}
		return rowValuesList;
	}

	public Object setValueAt(final Object rowIdObj, final PropertyName propertyName, final Object value)
	{
		final GridRowId rowId = GridRowId.of(rowIdObj);

		final GridRow row = rows.get(rowId);
		if (row == null)
		{
			return null;
		}
		return row.setValue(propertyName, value);
	}
	
	public Object getValueAt(final Object rowIdObj, final PropertyName propertyName)
	{
		final GridRowId rowId = GridRowId.of(rowIdObj);

		final GridRow row = rows.get(rowId);
		if (row == null)
		{
			return null;
		}

		return row.getValue(propertyName);
	}
}