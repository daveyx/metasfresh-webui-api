package de.metas.ui.web.window.model;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import de.metas.ui.web.window.PropertyName;
import de.metas.ui.web.window.WindowConstants.OnChangesFound;
import de.metas.ui.web.window.datasource.SaveResult;
import de.metas.ui.web.window.descriptor.PropertyDescriptor;
import de.metas.ui.web.window.model.action.ActionsList;
import de.metas.ui.web.window.shared.datatype.GridRowId;
import de.metas.ui.web.window.shared.datatype.PropertyPath;
import de.metas.ui.web.window.shared.datatype.PropertyValuesDTO;

/*
 * #%L
 * metasfresh-webui-api
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

/**
 * Class used to make sure all our DTO objects are serializable/deserializable to/from JSON.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class JSONProxyWindowModel implements WindowModel
{
	private final WindowModel delegate;

	private final ObjectMapper jsonObjectMapper = new ObjectMapper();

	public JSONProxyWindowModel(final WindowModel delegate)
	{
		super();
		this.delegate = delegate;

		jsonObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		jsonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty
		jsonObjectMapper.enableDefaultTyping();

		jsonObjectMapper.registerModule(new GuavaModule());

		// final SimpleModule module = new SimpleModule("test", new Version(1, 0, 0, null, null, null));
		// jsonObjectMapper.registerModule(module);
	}

	private Object testJSON(final Object valueObj)
	{
		Class<?> type = valueObj == null ? Object.class : valueObj.getClass();
		return testJSON(valueObj, type);
	}

	private <T> T testJSON(final Object valueObj, final Class<T> type)
	{
		String jsonStr = null;
		try
		{
			jsonStr = jsonObjectMapper.writeValueAsString(valueObj);
			System.out.println("------------------------------------------------------------------------");
			System.out.println("JSON: " + jsonStr);
			System.out.println("------------------------------------------------------------------------");
			final T value = jsonObjectMapper.readValue(jsonStr, type);
			return value;
		}
		catch (Exception ex)
		{
			final String errmsg = "Cannot serialize/deserialize value"
					+ "\n valueObj: " + valueObj
					+ "\n type: " + type
					+ "\n jsonStr: " + jsonStr;
			throw new IllegalArgumentException(errmsg, ex);
		}
	}

	@Override
	public void subscribe(final Object subscriberObj)
	{
		// TODO
		delegate.subscribe(subscriberObj);
	}

	@Override
	public void unsubscribe(final Object subscriberObj)
	{
		// TODO
		delegate.unsubscribe(subscriberObj);
	}

	@Override
	public void setRootPropertyDescriptor(final PropertyDescriptor rootPropertyDescriptor)
	{
		// TODO
		delegate.setRootPropertyDescriptor(rootPropertyDescriptor);
		// delegate.setRootPropertyDescriptor(testJSON(rootPropertyDescriptor, PropertyDescriptor.class));
	}

	@Override
	public boolean hasPreviousRecord()
	{
		// TODO
		return delegate.hasPreviousRecord();
	}

	@Override
	public void previousRecord(final OnChangesFound onChangesFound)
	{
		delegate.previousRecord(testJSON(onChangesFound, OnChangesFound.class));
	}

	@Override
	public boolean hasNextRecord()
	{
		// TODO
		return delegate.hasNextRecord();
	}

	@Override
	public void nextRecord(final OnChangesFound onChangesFound)
	{
		delegate.nextRecord(testJSON(onChangesFound, OnChangesFound.class));
	}

	@Override
	public PropertyValuesDTO getPropertyValuesDTO(final Set<PropertyName> selectedPropertyNames)
	{
		final PropertyValuesDTO values = delegate.getPropertyValuesDTO(selectedPropertyNames);
		return testJSON(values, PropertyValuesDTO.class);
	}

	@Override
	public boolean hasProperty(final PropertyPath propertyPath)
	{
		// TODO
		return delegate.hasProperty(testJSON(propertyPath, PropertyPath.class));
	}

	@Override
	public void setProperty(final PropertyPath propertyPath, final Object value)
	{
		delegate.setProperty( //
				testJSON(propertyPath, PropertyPath.class) //
				, testJSON(value) //
		);
	}

	@Override
	public Object getProperty(final PropertyPath propertyPath)
	{
		final Object value = delegate.getProperty(testJSON(propertyPath, PropertyPath.class));
		return testJSON(value);
	}

	@Override
	public Object getPropertyOrNull(final PropertyPath propertyPath)
	{
		final Object value = delegate.getPropertyOrNull(testJSON(propertyPath, PropertyPath.class));
		return testJSON(value);
	}

	@Override
	public void newRecordAsCopyById(final Object recordId)
	{
		delegate.newRecordAsCopyById(testJSON(recordId));
	}

	@Override
	public SaveResult saveRecord()
	{
		final SaveResult saveResult = delegate.saveRecord();
		return testJSON(saveResult, SaveResult.class);
	}

	@Override
	public GridRowId gridNewRow(final PropertyName gridPropertyName)
	{
		final GridRowId rowId = delegate.gridNewRow(testJSON(gridPropertyName, PropertyName.class));
		return testJSON(rowId, GridRowId.class);
	}

	@Override
	public ActionsList getActions()
	{
		final ActionsList actions = delegate.getActions();
		return testJSON(actions, ActionsList.class);
	}

	@Override
	public void executeAction(final String actionId)
	{
		delegate.executeAction(testJSON(actionId, String.class));
	}

	@Override
	public ActionsList getChildActions(String actionId)
	{
		final ActionsList actions = delegate.getChildActions(actionId);
		return testJSON(actions, ActionsList.class);
	}
}