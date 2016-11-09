package de.metas.ui.web.dashboard.json;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.metas.ui.web.dashboard.UserDashboardItem;
import de.metas.ui.web.window.datatypes.json.JSONFilteringOptions;

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

@SuppressWarnings("serial")
public class JSONDashboardItem implements Serializable
{
	/* package */static final JSONDashboardItem of(final UserDashboardItem dashboardItem, final JSONFilteringOptions jsonOpts)
	{
		return new JSONDashboardItem(dashboardItem, jsonOpts);
	}

	@JsonProperty("caption")
	private final String caption;
	@JsonProperty("url")
	private final String url;
	@JsonProperty("gridX")
	private final int gridX;
	@JsonProperty("gridY")
	private final int gridY;

	private JSONDashboardItem(final UserDashboardItem dashboardItem, final JSONFilteringOptions jsonOpts)
	{
		super();
		caption = dashboardItem.getCaption().translate(jsonOpts.getAD_Language());
		url = dashboardItem.getUrl();
		gridX = dashboardItem.getGridX();
		gridY = dashboardItem.getGridY();
	}

	public String getCaption()
	{
		return caption;
	}

	public String getUrl()
	{
		return url;
	}

	public int getGridX()
	{
		return gridX;
	}

	public int getGridY()
	{
		return gridY;
	}
}