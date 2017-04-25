package de.metas.ui.web.handlingunits;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.GuavaCollectors;
import org.adempiere.util.Services;
import org.adempiere.util.lang.ExtendedMemorizingSupplier;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import de.metas.handlingunits.model.I_M_HU;
import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.process.ProcessInstanceResult.SelectViewRowsAction;
import de.metas.ui.web.process.descriptor.ProcessLayout.ProcessLayoutType;
import de.metas.ui.web.process.view.ViewAction;
import de.metas.ui.web.process.view.ViewActionDescriptorsList;
import de.metas.ui.web.process.view.ViewActionParam;
import de.metas.ui.web.view.DocumentViewResult;
import de.metas.ui.web.view.IDocumentView;
import de.metas.ui.web.view.IDocumentViewSelection;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.event.DocumentViewChangesCollector;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.LookupValuesList;
import de.metas.ui.web.window.descriptor.DocumentFieldWidgetType;
import de.metas.ui.web.window.model.DocumentQueryOrderBy;
import de.metas.ui.web.window.model.filters.DocumentFilter;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

public class HUEditorView implements IDocumentViewSelection
{
	public static final Builder builder()
	{
		return new Builder();
	}

	public static HUEditorView cast(final IDocumentViewSelection view)
	{
		return (HUEditorView)view;
	}

	private final ViewId parentViewId;

	private final ViewId viewId;

	private final Set<DocumentPath> referencingDocumentPaths;

	private final CopyOnWriteArraySet<Integer> huIds;
	private final HUEditorViewRepository documentViewsLoader;
	private final ExtendedMemorizingSupplier<IndexedDocumentViews> _recordsSupplier = ExtendedMemorizingSupplier.of(() -> retrieveRecords());

	private final ViewActionDescriptorsList actions;

	private HUEditorView(final Builder builder)
	{
		super();

		parentViewId = builder.getParentViewId();

		viewId = builder.getViewId();

		huIds = new CopyOnWriteArraySet<>(builder.getHUIds());
		documentViewsLoader = builder.createDocumentViewsLoader();

		referencingDocumentPaths = builder.getReferencingDocumentPaths();

		this.actions = builder.actions;
	}

	@Override
	public ViewId getParentViewId()
	{
		return parentViewId;
	}

	@Override
	public ViewId getViewId()
	{
		return viewId;
	}

	@Override
	public String getTableName()
	{
		return I_M_HU.Table_Name;
	}

	@Override
	public long size()
	{
		return getRecords().size();
	}

	@Override
	public void close()
	{
		invalidateAllNoNotify();
	}

	@Override
	public int getQueryLimit()
	{
		return -1;
	}

	@Override
	public boolean isQueryLimitHit()
	{
		return false;
	}

	@Override
	public DocumentViewResult getPage(final int firstRow, final int pageLength, final List<DocumentQueryOrderBy> orderBys)
	{
		Stream<HUEditorRow> stream = getRecords().stream()
				.skip(firstRow)
				.limit(pageLength);

		final Comparator<HUEditorRow> comparator = createComparatorOrNull(orderBys);
		if (comparator != null)
		{
			stream = stream.sorted(comparator);
		}

		final List<IDocumentView> page = stream.collect(GuavaCollectors.toImmutableList());

		return DocumentViewResult.ofViewAndPage(this, firstRow, pageLength, orderBys, page);
	}

	@Override
	public ViewActionDescriptorsList getActions()
	{
		return actions;
	}

	private static final Comparator<HUEditorRow> createComparatorOrNull(final List<DocumentQueryOrderBy> orderBys)
	{
		if (orderBys == null || orderBys.isEmpty())
		{
			return null;
		}

		Comparator<HUEditorRow> comparator = null;
		for (final DocumentQueryOrderBy orderBy : orderBys)
		{
			final Comparator<HUEditorRow> orderByComparator = orderBy.<HUEditorRow> asComparator((viewRecord, fieldName) -> viewRecord.getFieldValueAsJson(fieldName));
			if (comparator == null)
			{
				comparator = orderByComparator;
			}
			else
			{
				comparator = comparator.thenComparing(orderByComparator);
			}
		}

		return comparator;
	}

	@Override
	public HUEditorRow getById(final DocumentId documentId) throws EntityNotFoundException
	{
		return getRecords().getById(documentId);
	}

	@Override
	public List<HUEditorRow> getByIds(final Set<DocumentId> documentIds)
	{
		return streamByIds(documentIds).collect(ImmutableList.toImmutableList());
	}

	@Override
	public LookupValuesList getFilterParameterDropdown(final String filterId, final String filterParameterName, final Evaluatee ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public LookupValuesList getFilterParameterTypeahead(final String filterId, final String filterParameterName, final String query, final Evaluatee ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<DocumentFilter> getStickyFilters()
	{
		return ImmutableList.of();
	}

	@Override
	public List<DocumentFilter> getFilters()
	{
		return ImmutableList.of();
	}

	@Override
	public List<DocumentQueryOrderBy> getDefaultOrderBys()
	{
		return ImmutableList.of();
	}

	@Override
	public String getSqlWhereClause(final Collection<DocumentId> viewDocumentIds)
	{
		Check.assumeNotEmpty(viewDocumentIds, "viewDocumentIds is not empty");
		// NOTE: ignoring non integer IDs because those might of HUStorage records, about which we don't care
		final Set<Integer> huIds = DocumentId.toIntSetIgnoringNonInts(viewDocumentIds);

		return I_M_HU.COLUMNNAME_M_HU_ID + " IN " + DB.buildSqlList(huIds);
	}

	@Override
	public boolean hasAttributesSupport()
	{
		return true;
	}

	public Set<DocumentPath> getReferencingDocumentPaths()
	{
		return referencingDocumentPaths;
	}

	public void invalidateAll()
	{
		invalidateAllNoNotify();

		DocumentViewChangesCollector.getCurrentOrAutoflush()
				.collectFullyChanged(this);
	}

	private void invalidateAllNoNotify()
	{
		_recordsSupplier.forget();
		documentViewsLoader.invalidateAll();
	}

	public void addHUsAndInvalidate(final Collection<I_M_HU> husToAdd)
	{
		if (husToAdd.isEmpty())
		{
			return;
		}

		huIds.addAll(extractHUIds(husToAdd));
		invalidateAll();
	}

	public void addHUAndInvalidate(final I_M_HU hu)
	{
		if (hu == null)
		{
			return;
		}

		huIds.add(hu.getM_HU_ID());
		invalidateAll();
	}

	public void removesHUsAndInvalidate(final Collection<I_M_HU> husToRemove)
	{
		if (husToRemove == null || husToRemove.isEmpty())
		{
			return;
		}

		huIds.removeAll(extractHUIds(husToRemove));
		invalidateAll();
	}

	private static final Set<Integer> extractHUIds(final Collection<I_M_HU> hus)
	{
		if (hus == null || hus.isEmpty())
		{
			return ImmutableSet.of();
		}

		return hus.stream().map(I_M_HU::getM_HU_ID).collect(Collectors.toSet());
	}

	@Override
	public void notifyRecordsChanged(final Set<TableRecordReference> recordRefs)
	{
		final IndexedDocumentViews records = getRecordsNoLoad();
		if (records == null)
		{
			return;
		}

		// TODO: notifyRecordsChanged:
		// get M_HU_IDs from recordRefs,
		// find the top level records from this view which contain our HUs
		// invalidate those top levels only

		final boolean containsSomeRecords = recordRefs.stream()
				.filter(recordRef -> I_M_HU.Table_Name.equals(recordRef.getTableName()))
				.map(recordRef -> DocumentId.of(recordRef.getRecord_ID()))
				.anyMatch(records::contains);
		if (!containsSomeRecords)
		{
			return;
		}

		invalidateAll();
	}

	private IndexedDocumentViews getRecords()
	{
		return _recordsSupplier.get();
	}

	private IndexedDocumentViews getRecordsNoLoad()
	{
		return _recordsSupplier.peek();
	}

	private IndexedDocumentViews retrieveRecords()
	{
		final List<HUEditorRow> recordsList = documentViewsLoader.retrieveDocumentViews(huIds);
		return new IndexedDocumentViews(recordsList);
	}

	@Override
	public Stream<HUEditorRow> streamByIds(final Collection<DocumentId> documentIds)
	{
		return getRecords().streamByIds(documentIds);
	}

	/** @return top level rows and included rows recursive stream */
	public Stream<HUEditorRow> streamAllRecursive()
	{
		return getRecords().streamRecursive();
	}

	@Override
	public <T> List<T> retrieveModelsByIds(final Collection<DocumentId> documentIds, final Class<T> modelClass)
	{
		final Set<Integer> huIds = getRecords()
				.streamByIds(documentIds)
				.filter(HUEditorRow::isPureHU)
				.map(HUEditorRow::getM_HU_ID)
				.collect(GuavaCollectors.toImmutableSet());
		if (huIds.isEmpty())
		{
			return ImmutableList.of();
		}

		final List<I_M_HU> hus = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_HU.class, Env.getCtx(), ITrx.TRXNAME_ThreadInherited)
				.addInArrayFilter(I_M_HU.COLUMN_M_HU_ID, huIds)
				.create()
				.list(I_M_HU.class);

		return InterfaceWrapperHelper.createList(hus, modelClass);
	}

	@ViewAction(caption = "Barcode", layoutType = ProcessLayoutType.SingleOverlayField)
	public SelectViewRowsAction actionSelectHUsByBarcode( //
			@ViewActionParam(caption = "Barcode", widgetType = DocumentFieldWidgetType.Text) final String barcode //
			, final Set<DocumentId> selectedDocumentIds //
			)
	{
		// Search for matching rowIds by barcode
		final Set<DocumentId> matchingRowIds = streamAllRecursive()
				.filter(row -> row.matchesBarcode(barcode))
				.map(row -> row.getDocumentId())
				.collect(ImmutableSet.toImmutableSet());
		if (matchingRowIds.isEmpty())
		{
			throw new AdempiereException("Nothing found for '" + barcode + "'");
		}

		// Join matching rowIds with currently selected ones
		final Set<DocumentId> rowIds = ImmutableSet.<DocumentId> builder()
				.addAll(matchingRowIds)
				// .addAll(selectedDocumentIds) // don't keep the previously selected ones (see https://github.com/metasfresh/metasfresh-webui-api/issues/313 )
				.build();

		return SelectViewRowsAction.builder()
				.viewId(getViewId())
				.rowIds(rowIds)
				.build();

	}

	//
	//
	//
	private static final class IndexedDocumentViews
	{
		/** Top level records list */
		private final List<HUEditorRow> records;
		/** All records (included ones too) indexed by DocumentId */
		private final Map<DocumentId, HUEditorRow> allRecordsById;

		public IndexedDocumentViews(final List<HUEditorRow> records)
		{
			super();
			this.records = ImmutableList.copyOf(records);
			allRecordsById = buildRecordsByIdMap(this.records);
		}

		public HUEditorRow getById(final DocumentId documentId)
		{
			final HUEditorRow record = allRecordsById.get(documentId);
			if (record == null)
			{
				throw new EntityNotFoundException("No document found for documentId=" + documentId);
			}
			return record;
		}

		public boolean contains(final DocumentId documentId)
		{
			return allRecordsById.containsKey(documentId);
		}

		public Stream<HUEditorRow> streamByIds(final Collection<DocumentId> documentIds)
		{
			if (documentIds == null || documentIds.isEmpty())
			{
				return Stream.empty();
			}

			return documentIds.stream()
					.distinct()
					.map(documentId -> allRecordsById.get(documentId))
					.filter(document -> document != null);
		}

		public Stream<HUEditorRow> stream()
		{
			return records.stream();
		}

		public Stream<HUEditorRow> streamRecursive()
		{
			return records.stream()
					.map(row -> streamRecursive(row))
					.reduce(Stream::concat)
					.orElse(Stream.of());
		}

		private Stream<HUEditorRow> streamRecursive(final HUEditorRow row)
		{
			return row.getIncludedDocuments()
					.stream()
					.map(includedRow -> streamRecursive(includedRow))
					.reduce(Stream.of(row), Stream::concat);
		}

		public long size()
		{
			return records.size();
		}

		private static ImmutableMap<DocumentId, HUEditorRow> buildRecordsByIdMap(final List<HUEditorRow> records)
		{
			if (records.isEmpty())
			{
				return ImmutableMap.of();
			}

			final ImmutableMap.Builder<DocumentId, HUEditorRow> recordsById = ImmutableMap.builder();
			records.forEach(record -> indexByIdRecursively(recordsById, record));
			return recordsById.build();
		}

		private static final void indexByIdRecursively(final ImmutableMap.Builder<DocumentId, HUEditorRow> collector, final HUEditorRow record)
		{
			collector.put(record.getDocumentId(), record);
			record.getIncludedDocuments()
					.forEach(includedRecord -> indexByIdRecursively(collector, includedRecord));
		}
	}

	//
	//
	//

	public static final class Builder
	{
		private ViewId parentViewId;
		private ViewId viewId;

		private String referencingTableName;
		private Set<DocumentPath> referencingDocumentPaths;

		private ViewActionDescriptorsList actions = ViewActionDescriptorsList.EMPTY;
		private Collection<Integer> huIds;

		private Builder()
		{
			super();
		}

		public HUEditorView build()
		{
			return new HUEditorView(this);
		}

		public Builder setParentViewId(final ViewId parentViewId)
		{
			this.parentViewId = parentViewId;
			return this;
		}

		private ViewId getParentViewId()
		{
			return parentViewId;
		}

		public Builder setViewId(final ViewId viewId)
		{
			this.viewId = viewId;
			return this;
		}

		public ViewId getViewId()
		{
			return viewId;
		}

		public Builder setHUIds(final Collection<Integer> huIds)
		{
			this.huIds = huIds;
			return this;
		}

		private Collection<Integer> getHUIds()
		{
			if (huIds == null || huIds.isEmpty())
			{
				return ImmutableSet.of();
			}
			return huIds;
		}

		private HUEditorViewRepository createDocumentViewsLoader()
		{
			return HUEditorViewRepository.builder()
					.windowId(getViewId().getWindowId())
					.referencingTableName(referencingTableName)
					.build();
		}

		public Builder setReferencingDocumentPaths(final String referencingTableName, final Set<DocumentPath> referencingDocumentPaths)
		{
			this.referencingTableName = referencingTableName;
			this.referencingDocumentPaths = referencingDocumentPaths;
			return this;
		}

		private Set<DocumentPath> getReferencingDocumentPaths()
		{
			return referencingDocumentPaths == null ? ImmutableSet.of() : ImmutableSet.copyOf(referencingDocumentPaths);
		}

		public Builder setActions(@NonNull final ViewActionDescriptorsList actions)
		{
			this.actions = actions;
			return this;
		}
	}
}