package com.commodule.datapermission;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionDataFieldReaderTest {

    private final ReflectionDataFieldReader reader = new ReflectionDataFieldReader();

    @Test
    void readsRecordAccessor() {
        assertThat(reader.read(new RecordSample(7L), "ownerId")).isEqualTo(7L);
    }

    @Test
    void readsGetterAndPrivateField() {
        assertThat(reader.read(new GetterSample(8L), "ownerId")).isEqualTo(8L);
        assertThat(reader.read(new FieldSample(9L), "ownerId")).isEqualTo(9L);
    }

    @Test
    void rejectsMissingField() {
        assertThatThrownBy(() -> reader.read(new RecordSample(7L), "missing"))
                .isInstanceOf(DataPermissionException.class);
    }

    private record RecordSample(Long ownerId) {
    }

    private static class GetterSample {

        private final Long ownerId;

        private GetterSample(Long ownerId) {
            this.ownerId = ownerId;
        }

        public Long getOwnerId() {
            return ownerId;
        }
    }

    private static class FieldSample {

        private final Long ownerId;

        private FieldSample(Long ownerId) {
            this.ownerId = ownerId;
        }
    }
}

