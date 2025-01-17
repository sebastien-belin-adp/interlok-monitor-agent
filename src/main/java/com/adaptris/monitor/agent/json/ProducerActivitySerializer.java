package com.adaptris.monitor.agent.json;

import java.lang.reflect.Type;

import com.adaptris.monitor.agent.activity.ProducerActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ProducerActivitySerializer implements JsonSerializer<ProducerActivity> {

	@Override
	public JsonElement serialize(ProducerActivity src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject object = new JsonObject();
		object.addProperty("uniqueId", src.getUniqueId());
		object.addProperty("msgCount", src.getMessageCount());
		object.addProperty("averageTimeTaken", src.getAvgMsTaken());
		// send back to Gson serializer
		return object;
	}

}
