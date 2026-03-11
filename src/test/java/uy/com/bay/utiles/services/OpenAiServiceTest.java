package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uy.com.bay.utiles.dto.AudioFile;

class OpenAiServiceTest {

    static class TestableOpenAiService extends OpenAiService {
        private final double simulatedDuration;
        private final String[] mockResponses;
        private int callCount = 0;

        public TestableOpenAiService(ObjectMapper objectMapper, double simulatedDuration, String... mockResponses) {
            super(objectMapper);
            this.simulatedDuration = simulatedDuration;
            this.mockResponses = mockResponses;
        }

        @Override
        protected double getDuration(File file) {
            return simulatedDuration;
        }

        @Override
        protected String callApi(byte[] bytes, String filename) {
            if (callCount < mockResponses.length) {
                return mockResponses[callCount++];
            }
            return "{}";
        }
    }

    @Test
    void testTranscribeAudioTotal_SplitAndMerge() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Chunk 1: Duration 1000s
        ObjectNode response1 = mapper.createObjectNode();
        response1.put("task", "transcribe");
        response1.put("language", "english");
        response1.put("duration", 1000.0);
        response1.put("text", "Hello world");
        ArrayNode segments1 = response1.putArray("segments");
        ObjectNode s1 = segments1.addObject();
        s1.put("start", 0.0);
        s1.put("end", 10.0);
        s1.put("text", "Hello world");
        s1.put("speaker", "speaker_0");

        // Chunk 2: Duration 500s
        ObjectNode response2 = mapper.createObjectNode();
        response2.put("task", "transcribe"); // Should be ignored in merge (taken from first)
        response2.put("language", "english");
        response2.put("duration", 500.0);
        response2.put("text", "Part two");
        ArrayNode segments2 = response2.putArray("segments");
        ObjectNode s2 = segments2.addObject();
        s2.put("start", 0.0); // Relative to chunk start
        s2.put("end", 5.0);
        s2.put("text", "Part two");
        s2.put("speaker", "speaker_1");

        // Total duration 2500s -> chunks logic:
        // target chunk = 1000s.
        // total = 2500s.
        // bytes = 10000.
        // bytesPerSecond = 4.
        // bytesPerChunk = 4 * 1000 = 4000.
        // chunks = ceil(10000 / 4000) = 3.
        // So we expect 3 calls.
        // Let's create a 3rd response.
        ObjectNode response3 = mapper.createObjectNode();
        response3.put("duration", 200.0);
        response3.put("text", "End");
        ArrayNode segments3 = response3.putArray("segments");
        ObjectNode s3 = segments3.addObject();
        s3.put("start", 0.0);
        s3.put("end", 2.0);
        s3.put("text", "End");

        TestableOpenAiService service = new TestableOpenAiService(mapper, 2500.0,
                mapper.writeValueAsString(response1),
                mapper.writeValueAsString(response2),
                mapper.writeValueAsString(response3));

        byte[] dummyBytes = new byte[10000]; // 10KB
        AudioFile audioFile = new AudioFile("test.mp3", new ByteArrayInputStream(dummyBytes));

        String result = service.transcribeAudioTotal(audioFile);

        JsonNode root = mapper.readTree(result);

        // Verify text concatenation
        assertEquals("Hello world Part two End", root.get("text").asText());

        // Verify total duration
        // 1000 + 500 + 200 = 1700
        assertEquals(1700.0, root.get("duration").asDouble(), 0.001);

        // Verify segments
        JsonNode segments = root.get("segments");
        assertEquals(3, segments.size());

        // Segment 1: 0-10
        assertEquals(0.0, segments.get(0).get("start").asDouble(), 0.001);
        assertEquals(10.0, segments.get(0).get("end").asDouble(), 0.001);

        // Segment 2: Offset by Chunk 1 duration (1000) -> 1000-1005
        assertEquals(1000.0, segments.get(1).get("start").asDouble(), 0.001);
        assertEquals(1005.0, segments.get(1).get("end").asDouble(), 0.001);

        // Segment 3: Offset by Chunk 1+2 duration (1000+500=1500) -> 1500-1502
        assertEquals(1500.0, segments.get(2).get("start").asDouble(), 0.001);
        assertEquals(1502.0, segments.get(2).get("end").asDouble(), 0.001);
    }
}
