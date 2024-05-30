package org.molgenis.strstats.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TsvLine {
    @CsvBindByName(
            column = "#chrom",
            required = true)
    String chrom;

    @CsvBindByName(
            column = "start",
            required = true)
    String pos;

    @CsvBindByName(
            column = "allele",
            required = true)
    String allele;
    @CsvBindByName(
            column = "strand",
            required = true)
    String strand;
}
