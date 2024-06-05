package org.molgenis.strstats.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputLine {
    String chrom;
    String pos;
    String id;
    String allele1_ru;
    String allele2_ru;
    String allele1_plus;
    String allele1_min;
    String allele2_plus;
    String allele2_min;

}

