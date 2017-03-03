package de.metas.ui.web.process;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.adempiere.util.Services;
import org.adempiere.util.api.IRangeAwareParams;
import org.adempiere.util.lang.IAutoCloseable;
import org.compiere.util.Env;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.metas.printing.esb.base.util.Check;
import de.metas.process.IADPInstanceDAO;
import de.metas.process.IProcessDefaultParametersProvider;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessDefaultParametersUpdater;
import de.metas.process.ProcessInfo;
import de.metas.ui.web.process.descriptor.ProcessDescriptor;
import de.metas.ui.web.process.descriptor.ProcessDescriptorsFactory;
import de.metas.ui.web.process.descriptor.ProcessParametersRepository;
import de.metas.ui.web.process.json.JSONCreateProcessInstanceRequest;
import de.metas.ui.web.view.IDocumentViewSelection;
import de.metas.ui.web.view.IDocumentViewsRepository;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.descriptor.DocumentEntityDescriptor;
import de.metas.ui.web.window.descriptor.factory.DocumentDescriptorFactory;
import de.metas.ui.web.window.model.Document;
import de.metas.ui.web.window.model.Document.CopyMode;

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

@Component
public class ProcessInstancesRepository
{
	//
	// Services
	@Autowired
	private ProcessDescriptorsFactory processDescriptorFactory;
	@Autowired
	private DocumentDescriptorFactory documentDescriptorFactory;
	@Autowired
	private IDocumentViewsRepository documentViewsRepo;

	private final LoadingCache<DocumentId, ProcessInstance> processInstances = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.removalListener(new RemovalListener<DocumentId, ProcessInstance>()
			{
				@Override
				public void onRemoval(final RemovalNotification<DocumentId, ProcessInstance> notification)
				{
					final ProcessInstance pinstance = notification.getValue();
					pinstance.destroy();
				}
			})
			.build(CacheLoader.from(adPInstanceId -> retrieveProcessInstance(adPInstanceId)));

	public void cacheReset()
	{
		processInstances.invalidateAll();
		processInstances.cleanUp();
	}

	public ProcessDescriptor getProcessDescriptor(final int adProcessId)
	{
		return processDescriptorFactory.getProcessDescriptor(adProcessId);
	}

	public ProcessInstance createNewProcessInstance(final int adProcessId, final JSONCreateProcessInstanceRequest request)
	{
		//
		// Save process info together with it's parameters and get the the newly created AD_PInstance_ID
		final ProcessInfo processInfo = createProcessInfo(adProcessId, request);
		Services.get(IADPInstanceDAO.class).saveProcessInfo(processInfo);
		final DocumentId adPInstanceId = DocumentId.of(processInfo.getAD_PInstance_ID());

		final Object processClassInstance = processInfo.newProcessClassInstanceOrNull();
		try (final IAutoCloseable c = JavaProcess.temporaryChangeCurrentInstance(processClassInstance))
		{
			//
			// Build the parameters document
			final ProcessDescriptor processDescriptor = getProcessDescriptor(adProcessId);
			final DocumentEntityDescriptor parametersDescriptor = processDescriptor.getParametersDescriptor();
			final Document parametersDoc = ProcessParametersRepository.instance.createNewParametersDocument(parametersDescriptor, adPInstanceId);
			final int windowNo = parametersDoc.getWindowNo();

			// Set parameters's default values
			ProcessDefaultParametersUpdater.newInstance()
					.addDefaultParametersProvider(processClassInstance instanceof IProcessDefaultParametersProvider ? (IProcessDefaultParametersProvider)processClassInstance : null)
					.onDefaultValue((parameter, value) -> parametersDoc.processValueChange(parameter.getColumnName(), value, () -> "default parameter value"))
					.updateDefaultValue(parametersDoc.getFieldViews(), field -> DocumentFieldAsProcessDefaultParameter.of(windowNo, field));

			//
			// Create (webui) process instance and add it to our internal cache.
			final ProcessInstance pinstance = ProcessInstance.builder()
					.setProcessDescriptor(processDescriptor)
					.setAD_PInstance_ID(adPInstanceId)
					.setParameters(parametersDoc)
					.setViewsRepo(documentViewsRepo)
					.setView(request.getViewId(), request.getViewDocumentIds())
					.setProcessClassInstance(processClassInstance)
					.build();
			processInstances.put(adPInstanceId, pinstance.copy(CopyMode.CheckInReadonly));
			return pinstance;
		}
	}

	private ProcessInfo createProcessInfo(final int adProcessId, final JSONCreateProcessInstanceRequest request)
	{
		// Validate request's AD_Process_ID
		// (we are not using it, but just for consistency)
		if (request.getAD_Process_ID() > 0 && request.getAD_Process_ID() != adProcessId)
		{
			throw new IllegalArgumentException("Request's AD_Process_ID is not valid. It shall be " + adProcessId + " or none but it was " + request.getAD_Process_ID());
		}

		Check.assume(adProcessId > 0, "adProcessId > 0");

		final String tableName;
		final int recordId;
		final String sqlWhereClause;

		//
		// View
		final String viewId = Strings.emptyToNull(request.getViewId());
		final String viewSelectedIdsAsStr;
		final DocumentPath singleDocumentPath = request.getSingleDocumentPath();
		if (!Check.isEmpty(viewId))
		{
			final IDocumentViewSelection view = documentViewsRepo.getView(viewId);
			final Set<DocumentId> viewDocumentIds = request.getViewDocumentIds();
			viewSelectedIdsAsStr = DocumentId.toCommaSeparatedString(viewDocumentIds);
			final int view_AD_Window_ID = view.getAD_Window_ID();
			tableName = documentDescriptorFactory.getTableNameOrNull(view_AD_Window_ID);

			if (viewDocumentIds.size() == 1)
			{
				final DocumentId singleDocumentId = viewDocumentIds.iterator().next();
				recordId = singleDocumentId.toIntOr(-1);
			}
			else
			{
				recordId = -1;
			}

			sqlWhereClause = view.getSqlWhereClause(viewDocumentIds);
		}
		//
		// Single document call
		else if (singleDocumentPath != null)
		{
			viewSelectedIdsAsStr = null;
			if (singleDocumentPath.isRootDocument())
			{
				tableName = documentDescriptorFactory.getTableNameOrNull(singleDocumentPath.getAD_Window_ID());
				recordId = singleDocumentPath.getDocumentId().toInt();
			}
			else
			{
				tableName = documentDescriptorFactory.getTableNameOrNull(singleDocumentPath.getAD_Window_ID(), singleDocumentPath.getDetailId());
				recordId = singleDocumentPath.getSingleRowId().toInt();
			}
			sqlWhereClause = null;
		}
		//
		// From menu
		else
		{
			viewSelectedIdsAsStr = null;
			tableName = null;
			recordId = -1;
			sqlWhereClause = null;
		}

		return ProcessInfo.builder()
				.setCtx(Env.getCtx())
				.setCreateTemporaryCtx()
				.setAD_Process_ID(adProcessId)
				.setRecord(tableName, recordId)
				.setWhereClause(sqlWhereClause)
				//
				.setLoadParametersFromDB(true) // important: we need to load the existing parameters from database, besides the internal ones we are adding here
				.addParameter(ProcessInstance.PARAM_ViewId, viewId) // internal parameter
				.addParameter(ProcessInstance.PARAM_ViewSelectedIds, viewSelectedIdsAsStr) // internal parameter
				//
				.build();
	}

	private ProcessInstance retrieveProcessInstance(final DocumentId adPInstanceId)
	{
		Check.assumeNotNull(adPInstanceId, "Parameter adPInstanceId is not null");
		Check.assume(adPInstanceId.toInt() > 0, "adPInstanceId > 0");

		//
		// Load process info
		final ProcessInfo processInfo = ProcessInfo.builder()
				.setCtx(Env.getCtx())
				.setCreateTemporaryCtx()
				.setAD_PInstance_ID(adPInstanceId.toInt())
				.build();

		final Object processClassInstance = processInfo.newProcessClassInstanceOrNull();
		try (final IAutoCloseable c = JavaProcess.temporaryChangeCurrentInstance(processClassInstance))
		{
			//
			// Build the parameters document
			final int adProcessId = processInfo.getAD_Process_ID();
			final ProcessDescriptor processDescriptor = getProcessDescriptor(adProcessId);

			//
			// Build the parameters (as document)
			final DocumentEntityDescriptor parametersDescriptor = processDescriptor.getParametersDescriptor();
			final Document parametersDoc = parametersDescriptor
					.getDataBinding()
					.getDocumentsRepository()
					.retrieveDocumentById(parametersDescriptor, adPInstanceId);

			// TODO: handle the case when the process was already executed
			// In that case we need to load the result and provide it to ProcessInstance constructor

			//
			// View informations
			final IRangeAwareParams processInfoParams = processInfo.getParameterAsIParams();
			final String viewId = processInfoParams.getParameterAsString(ProcessInstance.PARAM_ViewId);
			final String viewSelectedIdsStr = processInfoParams.getParameterAsString(ProcessInstance.PARAM_ViewSelectedIds);
			final Set<DocumentId> viewSelectedIds = DocumentId.ofCommaSeparatedString(viewSelectedIdsStr);

			//
			return ProcessInstance.builder()
					.setProcessDescriptor(processDescriptor)
					.setAD_PInstance_ID(adPInstanceId)
					.setParameters(parametersDoc)
					.setViewsRepo(documentViewsRepo)
					.setView(viewId, viewSelectedIds)
					.setProcessClassInstance(processClassInstance)
					.build();
		}
	}

	public <R> R forProcessInstanceReadonly(final int pinstanceIdAsInt, final Function<ProcessInstance, R> processor)
	{
		final DocumentId pinstanceId = DocumentId.of(pinstanceIdAsInt);

		try (final IAutoCloseable readLock = processInstances.getUnchecked(pinstanceId).lockForReading())
		{
			final ProcessInstance processInstance = processInstances.getUnchecked(pinstanceId);
			try (final IAutoCloseable c = processInstance.activate())
			{
				return processor.apply(processInstance);
			}
		}
	}

	public <R> R forProcessInstanceWritable(final int pinstanceIdAsInt, final Function<ProcessInstance, R> processor)
	{
		final DocumentId pinstanceId = DocumentId.of(pinstanceIdAsInt);

		try (final IAutoCloseable writeLock = processInstances.getUnchecked(pinstanceId).lockForWriting())
		{
			final ProcessInstance processInstance = processInstances.getUnchecked(pinstanceId).copy(CopyMode.CheckOutWritable);

			// Make sure the process was not already executed.
			// If it was executed we are not allowed to change it.
			processInstance.assertNotExecuted();

			try (final IAutoCloseable c = processInstance.activate())
			{
				// Call the given processor to apply changes to this process instance.
				final R result = processor.apply(processInstance);

				// Actually put it back
				processInstance.saveIfValidAndHasChanges(false); // throwEx=false
				processInstances.put(pinstanceId, processInstance.copy(CopyMode.CheckInReadonly));

				return result;
			}
		}
	}
}
