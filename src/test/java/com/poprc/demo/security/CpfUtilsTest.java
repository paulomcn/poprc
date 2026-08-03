package com.poprc.demo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfUtilsTest {
    @Test
    void deveNormalizarEValidarCpf() {
        assertEquals("52998224725", CpfUtils.normalizar("529.982.247-25"));
        assertTrue(CpfUtils.valido("529.982.247-25"));
    }

    @Test
    void deveRejeitarCpfInvalidoOuRepetido() {
        assertFalse(CpfUtils.valido("111.111.111-11"));
        assertFalse(CpfUtils.valido("529.982.247-24"));
        assertFalse(CpfUtils.valido("123"));
        assertNull(CpfUtils.normalizar(null));
    }
}
