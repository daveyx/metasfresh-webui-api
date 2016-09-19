package de.metas.ui.web.window.model.lookup;

import org.compiere.util.Evaluatee;

import de.metas.ui.web.window.datatypes.LookupValue;
import de.metas.ui.web.window.datatypes.LookupValuesList;

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

public interface LookupDataSource
{
	int FIRST_ROW = 0;
	int DEFAULT_PageLength = 10;

	LookupValuesList findEntities(Evaluatee ctx, String filter, int firstRow, int pageLength);

	LookupValuesList findEntities(Evaluatee ctx, int pageLength);

	LookupValue findById(Object id);

	boolean isNumericKey();

	/**
	 * Set this data source as staled.
	 *
	 * @param triggeringFieldName field name which triggered this request (optional)
	 * @return true if this lookup source was marked as staled
	 */
	boolean setStaled(final String triggeringFieldName);

	boolean isStaled();

	LookupDataSource copy();
}