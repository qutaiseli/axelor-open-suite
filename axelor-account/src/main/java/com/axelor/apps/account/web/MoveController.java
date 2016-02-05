/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.web;

import java.util.List;

import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.move.MoveService;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class MoveController {
	
	@Inject
	protected MoveService moveService;
	
	@Inject
	protected MoveRepository moveRepo;
	
	public void validate(ActionRequest request, ActionResponse response) {

		Move move = request.getContext().asType(Move.class);
		move = moveRepo.find(move.getId());
		
		try {
			moveService.getMoveValidateService().validate(move);
			response.setReload(true);
		}
		catch (Exception e){ TraceBackService.trace(response, e); }
	}
	
	public void getPeriod(ActionRequest request, ActionResponse response) {
		
		Move move = request.getContext().asType(Move.class);
	
		try {
			if(move.getDate() != null && move.getCompany() != null) {
				
				response.setValue("period", Beans.get(PeriodService.class).rightPeriod(move.getDate(), move.getCompany()));				
			}
			else {
				response.setValue("period", null);
			}
		}
		catch (Exception e){ TraceBackService.trace(response, e); }
	}
	
	public void generateReverse(ActionRequest request, ActionResponse response) {
		
		Move move = request.getContext().asType(Move.class);
		
		try {
			Move newMove = moveService.generateReverse(moveRepo.find(move.getId()));
			if(newMove != null){
				response.setView(ActionView
							.define(I18n.get("Account move"))
							.model("com.axelor.apps.account.db.Move")
							.param("forceEdit", "true")
							.context("_showRecord", newMove.getId().toString())
							.map());
			}
		}
		catch (Exception e){ TraceBackService.trace(response, e); }
	}
	
	@SuppressWarnings("unchecked")
	public void validateMultipleMoves(ActionRequest request, ActionResponse response){
		List<Long> moveIds = (List<Long>) request.getContext().get("_ids");
		if(!moveIds.isEmpty()){
			List<? extends Move> moveList = moveRepo.all().filter("self.id in ?1 AND self.statusSelect NOT IN (?2, ?3)", moveIds, MoveRepository.STATUS_VALIDATED, MoveRepository.STATUS_CANCELED).fetch();
			if(!moveList.isEmpty()){
				boolean error = moveService.getMoveValidateService().validateMultiple(moveList);
				if(error)
					response.setFlash(I18n.get(IExceptionMessage.MOVE_VALIDATION_NOT_OK));
				else{
					response.setFlash(I18n.get(IExceptionMessage.MOVE_VALIDATION_OK));
					response.setReload(true);
				}
			}
			else response.setFlash(I18n.get(IExceptionMessage.NO_MOVES_SELECTED));
		}
		else response.setFlash(I18n.get(IExceptionMessage.NO_MOVES_SELECTED));
	}
}
