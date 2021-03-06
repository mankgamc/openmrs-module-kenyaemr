package org.openmrs.module.kenyaemr.calculation.library.hiv.art;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.openmrs.Obs;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.calculation.result.ListResult;
import org.openmrs.calculation.result.SimpleResult;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.CalculationUtils;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.models.Cd4ValueAndDate;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.common.DurationUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by codehub on 06/07/15.
 */
public class ViralLoadCalculation extends AbstractPatientCalculation {

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues,PatientCalculationContext context) {

        CalculationResultMap ret = new CalculationResultMap();
        CalculationResultMap artStartDateMap = calculate(new InitialArtStartDateCalculation(), cohort, context);

        Integer outcomePeriod = (parameterValues != null && parameterValues.containsKey("outcomePeriod")) ? (Integer) parameterValues.get("outcomePeriod") : null;
        if(outcomePeriod != null) {
            context.setNow(DateUtil.adjustDate(context.getNow(), outcomePeriod, DurationUnit.MONTHS));
        }

        CalculationResultMap allVL = Calculations.allObs(Dictionary.getConcept(Dictionary.HIV_VIRAL_LOAD), cohort, context);


        for(Integer ptId:cohort) {
            Cd4ValueAndDate vlValue = null;
            ListResult listResult = (ListResult) allVL.get(ptId);
            List<Obs> obsList = CalculationUtils.extractResultValues(listResult);
            List<Obs> validVls = new ArrayList<Obs>();
            Date artStartDate = EmrCalculationUtils.datetimeResultForPatient(artStartDateMap, ptId);
            if(obsList.size() > 0 && artStartDate != null && outcomePeriod != null) {

                Date outcomeDate = DateUtil.adjustDate(DateUtil.adjustDate(artStartDate, outcomePeriod, DurationUnit.MONTHS), 1, DurationUnit.DAYS);


                for(Obs obs: obsList) {
                    if(obs.getObsDatetime().before(outcomeDate) && obs.getObsDatetime().after(dateLimit(outcomeDate, -366))) {
                        validVls.add(obs);
                    }
                }

                if(validVls.size() > 0) {
                    vlValue = new Cd4ValueAndDate(validVls.get(validVls.size() -1).getValueNumeric(),validVls.get(validVls.size() -1).getObsDatetime() );
                }
                ret.put(ptId, new SimpleResult(vlValue, this));
            }

        }
        return ret;
    }

    private  Date dateLimit(Date date1, Integer days) {

        return DateUtil.adjustDate(date1, days, DurationUnit.DAYS);
    }
}
