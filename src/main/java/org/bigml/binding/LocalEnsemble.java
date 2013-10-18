/*
 An local Ensemble object.

This module defines an Ensemble to make predictions locally using its
associated models.

This module can not only save you a few credits, but also enormously
reduce the latency for each prediction and let you use your models
offline.

import org.bigml.binding.BigMLClient;
import org.bigml.binding.resources.Ensemble;


# creating ensemble
Ensemble ensemble = BigMLCliente.getInstance().createEnsemble('dataset/5143a51a37203f2cf7000972')

# Ensemble object to predict
LocalEnsemble localEnsemble = LocalEnsemble(ensemble, storage='./storage')
localEnsemble.predict({"petal length": 3, "petal width": 1})
 */

package org.bigml.binding;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bigml.binding.utils.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A local predictive Ensemble.
 * 
 * Uses a number of BigML remote models to build an ensemble local version
 * that can be used to generate predictions locally.
 *
 */
public class LocalEnsemble {
	
	/**
	 * Logging
	 */
	static Logger logger = Logger.getLogger(LocalEnsemble.class.getName());
	
	
	private String storage;
	private String ensembleId;
	
	private String[] modelsIds;
	private String[] modelsSplit;
	private JSONArray models;
	private MultiModel multiModel;
	
	
	/** 
	 * Constructor
	 * 
	 * @param ensemble	the json representation for the remote ensemble
	 */
	public LocalEnsemble(JSONObject ensemble, final String storage, final Integer maxModels) throws Exception {
		super();

		this.storage = storage!=null ? storage : "./storage";
		this.ensembleId = null;
		
		if (ensemble.get("objects")!=null) {
			try {
				// TODO: list of models
				// models = [get_model_id(model) for model in ensemble]
			} catch (Exception e) {
				logger.error("'Failed to verify the list of models. Check your model id values", e);
				throw new Exception();
			}
		} else {
			this.ensembleId = (String) ensemble.get("resource");
			JSONArray modelsJson = (JSONArray) Utils.getJSONObject(ensemble, "object.models");
			modelsIds = (String[]) new String[modelsJson.size()];
			for (int i=0; i<modelsJson.size(); i++) {
				modelsIds[i] = (String) modelsJson.get(i);
			}
		}
		
		int numberOfModels = modelsIds.length;
		if (maxModels == null) {
			modelsSplit = modelsIds;
		} else {
			int maxLength = numberOfModels>maxModels ? maxModels : numberOfModels;
			modelsSplit = Arrays.copyOfRange(modelsIds, 0, maxLength);	
		}

		BigMLClient bigmlClient = BigMLClient.getInstance(storage);
		models = new JSONArray();
		
		for (int i=0; i<modelsSplit.length; i++) {
			String modelId = (String) modelsSplit[i];
			models.add(bigmlClient.getModel(modelId));
		}
		multiModel = new MultiModel(models);
	}
	
		
	/**
	 * Lists all the model/ids that compound the ensemble.
	 */
	public String[] listModels() {
		return this.modelsIds;
	}
	
	
	/**
	 * Makes a prediction based on the prediction made by every model.
	 *
     * The method parameter is a numeric key to the following combination
     * methods in classifications/regressions:
     *    0 - majority vote (plurality)/ average: PLURALITY_CODE
     *    1 - confidence weighted majority vote / error weighted:
     *          CONFIDENCE_CODE
     *    2 - probability weighted majority vote / average:
     *          PROBABILITY_CODE
	 */
	public HashMap<String, Object> predict(final String inputData, Boolean byName, Integer method, Boolean withConfidence) throws Exception {
		if (method == null) {
			method = MultiVote.PLURALITY;
		}
		if (byName == null) {
			byName = true;
		}
		if (withConfidence == null) {
			withConfidence = false;
		}

		MultiVote votes = this.multiModel.generateVotes(inputData, byName, withConfidence);
		HashMap<String, Object> combinedPrediction = votes.combine(method, withConfidence);
		
	    return combinedPrediction;
	}

}