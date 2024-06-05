package org.molgenis.strstats.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Key {
    String chrom;
    String pos;
    String allele;
    Strand strand;
    String ru;

}