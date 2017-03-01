package de.metas.ui.web.window.datatypes.json;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.adempiere.util.GuavaCollectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import de.metas.ui.web.window.datatypes.json.filters.JSONDocumentFilterDescriptor;
import de.metas.ui.web.window.descriptor.DetailId;
import de.metas.ui.web.window.descriptor.DocumentLayoutDetailDescriptor;
import de.metas.ui.web.window.descriptor.filters.DocumentFilterDescriptor;
import io.swagger.annotations.ApiModel;

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

@ApiModel("tab")
@SuppressWarnings("serial")
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public final class JSONDocumentLayoutTab implements Serializable
{
	static List<JSONDocumentLayoutTab> ofList(final Collection<DocumentLayoutDetailDescriptor> details, final JSONOptions jsonOpts)
	{
		final Collection<DocumentFilterDescriptor> filters = null;
		return details.stream()
				.map(detail -> of(detail, filters, jsonOpts))
				.filter(jsonDetail -> jsonDetail.hasElements())
				.collect(GuavaCollectors.toImmutableList());
	}

	public static JSONDocumentLayoutTab of(
			final DocumentLayoutDetailDescriptor detail //
			, final Collection<DocumentFilterDescriptor> filters //
			, final JSONOptions jsonOpts //
	)
	{
		return new JSONDocumentLayoutTab(detail, filters, jsonOpts);
	}

	/** i.e. AD_Window_ID */
	@JsonProperty("type")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String type;

	@JsonProperty("tabid")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String tabid;

	@JsonProperty("caption")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String caption;

	@JsonProperty("description")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String description;

	@JsonProperty("emptyResultText")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultText;

	@JsonProperty("emptyResultHint")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String emptyResultHint;

	@JsonProperty("elements")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentLayoutElement> elements;

	@JsonProperty("filters")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<JSONDocumentFilterDescriptor> filters;

	@JsonProperty("supportQuickInput")
	private final boolean supportQuickInput;

	@JsonProperty("queryOnActivate")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final boolean queryOnActivate;

	private JSONDocumentLayoutTab(
			final DocumentLayoutDetailDescriptor detail //
			, final Collection<DocumentFilterDescriptor> filters //
			, final JSONOptions jsonOpts //
	)
	{
		super();

		type = String.valueOf(detail.getAD_Window_ID());

		final DetailId detailId = detail.getDetailId();
		tabid = DetailId.toJson(detailId);

		final String adLanguage = jsonOpts.getAD_Language();
		if (jsonOpts.isDebugShowColumnNamesForCaption() && tabid != null)
		{
			caption = new StringBuilder()
					.append("[")
					.append(tabid)
					.append(detail.isQueryOnActivate() ? "Q" : "")
					.append("] ")
					.append(detail.getCaption(adLanguage))
					.toString();
		}
		else
		{
			caption = detail.getCaption(adLanguage);
		}
		description = detail.getDescription(adLanguage);
		emptyResultText = detail.getEmptyResultText(adLanguage);
		emptyResultHint = detail.getEmptyResultHint(adLanguage);

		elements = JSONDocumentLayoutElement.ofList(detail.getElements(), jsonOpts);

		this.filters = JSONDocumentFilterDescriptor.ofCollection(filters, jsonOpts);

		supportQuickInput = detail.isSupportQuickInput();
		queryOnActivate = detail.isQueryOnActivate();
	}

	@JsonCreator
	private JSONDocumentLayoutTab(
			@JsonProperty("type") final String type //
			, @JsonProperty("tabid") final String tabid //
			, @JsonProperty("caption") final String caption //
			, @JsonProperty("description") final String description //
			, @JsonProperty("emptyResultText") final String emptyResultText //
			, @JsonProperty("emptyResultHint") final String emptyResultHint //
			, @JsonProperty("elements") final List<JSONDocumentLayoutElement> elements //
			, @JsonProperty("filters") final List<JSONDocumentFilterDescriptor> filters //
			, @JsonProperty("supportQuickInput") final boolean supportQuickInput //
			, @JsonProperty("queryOnActivate") final boolean queryOnActivate //
	)
	{
		super();
		this.type = type;
		this.tabid = tabid;

		this.caption = caption;
		this.description = description;
		this.emptyResultText = emptyResultText;
		this.emptyResultHint = emptyResultHint;

		this.elements = elements == null ? ImmutableList.of() : ImmutableList.copyOf(elements);
		this.filters = filters == null ? ImmutableList.of() : ImmutableList.copyOf(filters);
		this.supportQuickInput = supportQuickInput;
		this.queryOnActivate = queryOnActivate;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.add("tabid", tabid)
				.add("caption", caption)
				.add("elements", elements.isEmpty() ? null : elements)
				.add("filters", filters.isEmpty() ? null : filters)
				.toString();
	}

	public String getTabid()
	{
		return tabid;
	}

	public String getCaption()
	{
		return caption;
	}

	public String getDescription()
	{
		return description;
	}

	public String getEmptyResultText()
	{
		return emptyResultText;
	}

	public String getEmptyResultHint()
	{
		return emptyResultHint;
	}

	public List<JSONDocumentLayoutElement> getElements()
	{
		return elements;
	}

	public boolean hasElements()
	{
		return !elements.isEmpty();
	}

	public List<JSONDocumentFilterDescriptor> getFilters()
	{
		return filters;
	}
}
