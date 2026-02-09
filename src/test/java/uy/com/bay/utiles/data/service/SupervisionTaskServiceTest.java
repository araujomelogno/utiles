package uy.com.bay.utiles.data.service;

import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupervisionTaskServiceTest {

    @Mock
    private SupervisionTaskRepository repository;

    private SupervisionTaskService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SupervisionTaskService(repository);
    }

    @Test
    void testFindDTOByCreatedBetweenAndFileNameAndStatus() {
        // Mock Tuple 1 (Speaker 1)
        Tuple tuple1 = mock(Tuple.class);
        when(tuple1.get("id", Long.class)).thenReturn(1L);
        when(tuple1.get("fileName", String.class)).thenReturn("file1.mp3");
        when(tuple1.get("status", Status.class)).thenReturn(Status.DONE);
        when(tuple1.get("aiScore", Double.class)).thenReturn(0.85);
        when(tuple1.get("totalAudioDuration", Double.class)).thenReturn(100.0);
        when(tuple1.get("speakingDuration", Double.class)).thenReturn(80.0);
        when(tuple1.get("created", Date.class)).thenReturn(new Date());
        when(tuple1.get("output", String.class)).thenReturn("Output");
        when(tuple1.get("evaluationOutput", String.class)).thenReturn("Eval");
        when(tuple1.get("speaker", String.class)).thenReturn("Speaker1");
        when(tuple1.get("duration", Double.class)).thenReturn(50.0);

        // Mock Tuple 2 (Speaker 2 for same task)
        Tuple tuple2 = mock(Tuple.class);
        when(tuple2.get("id", Long.class)).thenReturn(1L);
        when(tuple2.get("fileName", String.class)).thenReturn("file1.mp3");
        when(tuple2.get("status", Status.class)).thenReturn(Status.DONE);
        when(tuple2.get("aiScore", Double.class)).thenReturn(0.85);
        when(tuple2.get("totalAudioDuration", Double.class)).thenReturn(100.0);
        when(tuple2.get("speakingDuration", Double.class)).thenReturn(80.0);
        when(tuple2.get("created", Date.class)).thenReturn(new Date());
        when(tuple2.get("output", String.class)).thenReturn("Output");
        when(tuple2.get("evaluationOutput", String.class)).thenReturn("Eval");
        when(tuple2.get("speaker", String.class)).thenReturn("Speaker2");
        when(tuple2.get("duration", Double.class)).thenReturn(30.0);

        when(repository.findTuplesByCreatedBetweenOrderByCreatedDesc(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(tuple1, tuple2));

        List<SupervisionTaskDTO> results = service.findDTOByCreatedBetweenAndFileNameAndStatus(new Date(), new Date(), null, null);

        assertEquals(1, results.size());
        SupervisionTaskDTO dto = results.get(0);
        assertEquals(1L, dto.getId());
        assertEquals("file1.mp3", dto.getFileName());
        assertEquals(Status.DONE, dto.getStatus());
        assertEquals(0.85, dto.getAiScore());
        assertEquals(100.0, dto.getTotalAudioDuration());
        assertEquals(80.0, dto.getSpeakingDuration());
        assertEquals("Output", dto.getOutput());
        assertEquals("Eval", dto.getEvaluationOutput());

        assertEquals(2, dto.getDurationBySpeakers().size());
        assertEquals(50.0, dto.getDurationBySpeakers().get("Speaker1"));
        assertEquals(30.0, dto.getDurationBySpeakers().get("Speaker2"));
    }
}
