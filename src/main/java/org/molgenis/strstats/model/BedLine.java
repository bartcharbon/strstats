package org.molgenis.strstats.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BedLine {
    @CsvBindByPosition(
            position = 0,
            required = true)
    String chrom;

    @CsvBindByPosition(
            position = 1,
            required = true)
    String pos;

    @CsvBindByPosition(
            position = 4,
            required = true)
    String id;

}
