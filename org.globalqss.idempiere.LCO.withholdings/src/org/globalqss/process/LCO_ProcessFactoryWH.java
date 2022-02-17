/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Carlos Ruiz - globalqss                                           *
**********************************************************************/

package org.globalqss.process;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import dev.itechsolutions.process.GenerateInvoiceWithholding;
import dev.itechsolutions.process.GenerateTaxDeclare;
import dev.itechsolutions.process.ITS_GenerateTxtSeniat;
import dev.itechsolutions.process.ITS_GenerateXmlSeniat;

public class LCO_ProcessFactoryWH implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		ProcessCall process = null;
		if ("org.globalqss.process.LCO_GenerateWithholding".equals(className)) {
			try {
				process =  LCO_GenerateWithholding.class.getConstructor().newInstance();
			} catch (Exception e) {}
		} else if ("org.globalqss.process.LCO_CreateWithholdingReversal".equals(className)) {
			try {
				process =  LCO_CreateWithholdingReversal.class.getConstructor().newInstance();
			} catch (Exception e) {}
		}
		else if (GenerateInvoiceWithholding.class.getCanonicalName().equals(className))
		{
			try {
				process = GenerateInvoiceWithholding.class.getConstructor().newInstance();
			} catch (Exception e) {}
		}
		else if (GenerateTaxDeclare.class.getCanonicalName().equals(className))
		{
			try {
				process = GenerateTaxDeclare.class.getDeclaredConstructor().newInstance();
			} catch (Exception e) {}
		}
		else if (ITS_GenerateTxtSeniat.class.getCanonicalName().equals(className))
		{
			try {
				process = ITS_GenerateTxtSeniat.class.getDeclaredConstructor().newInstance();
			} catch (Exception e) {}
		}
		else if (ITS_GenerateXmlSeniat.class.getCanonicalName().equals(className))
		{
			try {
				process = ITS_GenerateXmlSeniat.class.getDeclaredConstructor().newInstance();
			} catch (Exception e) {}
		}
		
		return process;
		
	}

}
