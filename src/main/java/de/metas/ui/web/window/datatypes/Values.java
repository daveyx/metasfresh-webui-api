package de.metas.ui.web.window.datatypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.compiere.util.NamePair;

import de.metas.ui.web.window.datatypes.json.JSONDate;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Misc JSON values converters.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public final class Values
{
	public static final Object valueToJsonObject(final Object value)
	{
		return valueToJsonObject(value, UnaryOperator.identity());
	}

	/**
	 * Convert value to JSON.
	 * 
	 * @param value
	 * @param fallbackMapper mapper called when value could not be converted to JSON; takes as input the <code>value</code>
	 * @return JSON value
	 */
	public static final Object valueToJsonObject(final Object value, final UnaryOperator<Object> fallbackMapper)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof java.util.Date)
		{
			final java.util.Date valueDate = (java.util.Date)value;
			return JSONDate.toJson(valueDate);
		}
		else if (value instanceof LookupValue)
		{
			final LookupValue lookupValue = (LookupValue)value;
			return JSONLookupValue.ofLookupValue(lookupValue);
		}
		else if (value instanceof NamePair)
		{
			final NamePair lookupValue = (NamePair)value;
			return JSONLookupValue.ofNamePair(lookupValue);
		}
		else if (value instanceof BigDecimal)
		{
			// NOTE: because javascript cannot distinguish between "1.00" and "1.0" as number,
			// we need to provide the BigDecimals as Strings.
			return value.toString();
		}
		else if (value instanceof DocumentId)
		{
			return ((DocumentId)value).toJson();
		}
		else if (value instanceof Collection)
		{
			final Collection<?> valuesList = (Collection<?>)value;
			return valuesList.stream()
					.map(v -> valueToJsonObject(v, fallbackMapper))
					.collect(Collectors.toCollection(ArrayList::new)); // don't use ImmutableList because we might get null values
		}
		else
		{
			return fallbackMapper.apply(value);
		}
	}

	private Values()
	{
		throw new UnsupportedOperationException();
	}
}
