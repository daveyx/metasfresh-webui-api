package de.metas.ui.web.process.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.metas.process.ProcessPreconditionsResolution;
import de.metas.ui.web.view.IView;
import de.metas.ui.web.window.datatypes.DocumentIdsSelection;
import de.metas.ui.web.window.datatypes.PanelLayoutType;

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

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ViewAction
{
	String caption();

	String description() default "";

	boolean defaultAction() default false;
	
	PanelLayoutType layoutType() default PanelLayoutType.Panel;

	Class<? extends Precondition> precondition() default AlwaysAllowPrecondition.class;

	public interface Precondition
	{
		ProcessPreconditionsResolution matches(IView view, DocumentIdsSelection selectedDocumentIds);
	}

	public static final class AlwaysAllowPrecondition implements Precondition
	{
		public static final transient ViewAction.AlwaysAllowPrecondition instance = new ViewAction.AlwaysAllowPrecondition();

		@Override
		public ProcessPreconditionsResolution matches(IView view, DocumentIdsSelection selectedDocumentIds)
		{
			return ProcessPreconditionsResolution.accept();
		}
	}
}
