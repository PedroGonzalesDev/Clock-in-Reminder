package com.clockinreminder.app.export

import android.content.Context
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.MonthSummaryResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

object ExcelExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Generates a Microsoft Excel XML Spreadsheet (.xls / .xml).
     */
    fun exportMonthToExcel(
        context: Context,
        companyName: String,
        employeeName: String,
        summary: MonthSummaryResult
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "espelho_ponto_${summary.yearMonth.year}_${summary.yearMonth.monthValue}.xls"
        val file = File(exportDir, filename)

        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
            writer.write("""<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#121B22"/>
  </Style>
  <Style ss:ID="HeaderTitle">
   <Font ss:FontName="Segoe UI" ss:Size="14" ss:Bold="1" ss:Color="#142431"/>
  </Style>
  <Style ss:ID="HeaderMeta">
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#187B77"/>
  </Style>
  <Style ss:ID="TableHeader">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#187B77"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#DFDACF"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#142431" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="DataCellCenter">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#DFDACF"/>
   </Borders>
  </Style>
  <Style ss:ID="DataCellBold">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#142431"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#DFDACF"/>
   </Borders>
  </Style>
  <Style ss:ID="DataCellLeft">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#DFDACF"/>
   </Borders>
  </Style>
  <Style ss:ID="TotalHeader">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="11" ss:Bold="1" ss:Color="#142431"/>
   <Interior ss:Color="#EFECE3" ss:Pattern="Solid"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Horas">
  <Table ss:DefaultColumnWidth="75">
   <Column ss:Width="70"/>
   <Column ss:Width="50"/>
   <Column ss:Width="75"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="55"/>
   <Column ss:Width="65"/>
   <Column ss:Width="65"/>
   <Column ss:Width="65"/>
   <Column ss:Width="65"/>
   <Column ss:Width="65"/>
   <Column ss:Width="180"/>
   <Row ss:Height="24">
    <Cell ss:StyleID="HeaderTitle"><Data ss:Type="String">CLOCK IN REMINDER - ESPELHO DE PONTO</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="HeaderMeta"><Data ss:Type="String">Empresa: $companyName | Funcionário: $employeeName | Competência: ${summary.yearMonth.monthValue}/${summary.yearMonth.year}</Data></Cell>
   </Row>
   <Row ss:Index="4" ss:Height="22">
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Data</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Dia</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Tipo</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Entrada 1</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Saída 1</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Entrada 2</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Saída 2</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Entrada 3</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Saída 3</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Entrada 4</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Saída 4</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Trabalhadas</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Extra Útil</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Extra 60%</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Extra 100%</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Noturnas</Data></Cell>
    <Cell ss:StyleID="TableHeader"><Data ss:Type="String">Observações</Data></Cell>
   </Row>
""")

            val dayOfWeekAbbr = mapOf(
                java.time.DayOfWeek.MONDAY to "Seg",
                java.time.DayOfWeek.TUESDAY to "Ter",
                java.time.DayOfWeek.WEDNESDAY to "Qua",
                java.time.DayOfWeek.THURSDAY to "Qui",
                java.time.DayOfWeek.FRIDAY to "Sex",
                java.time.DayOfWeek.SATURDAY to "Sáb",
                java.time.DayOfWeek.SUNDAY to "Dom"
            )

            for (day in summary.dayResults) {
                val punches = day.punches
                val e1 = punches.getOrNull(0)?.time?.toString() ?: ""
                val s1 = punches.getOrNull(1)?.time?.toString() ?: ""
                val e2 = punches.getOrNull(2)?.time?.toString() ?: ""
                val s2 = punches.getOrNull(3)?.time?.toString() ?: ""
                val e3 = punches.getOrNull(4)?.time?.toString() ?: ""
                val s3 = punches.getOrNull(5)?.time?.toString() ?: ""
                val e4 = punches.getOrNull(6)?.time?.toString() ?: ""
                val s4 = punches.getOrNull(7)?.time?.toString() ?: ""

                val punchNotes = day.punches.mapNotNull { it.note?.trim() }.filter { it.isNotEmpty() }
                val allNotes = mutableListOf<String>()
                if (day.holiday != null) allNotes.add(day.holiday.name)
                if (day.occurrence != null && !day.occurrence.note.isNullOrBlank()) allNotes.add(day.occurrence.note)
                allNotes.addAll(punchNotes)
                if (day.punches.size > 8) {
                    val extraPunches = day.punches.drop(8).joinToString(", ") { "${it.time}" }
                    allNotes.add("Outras: $extraPunches")
                }
                val obs = allNotes.joinToString(" / ")
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

                writer.write("""   <Row ss:Height="18">
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${day.date.format(dateFormatter)}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${dayOfWeekAbbr[day.date.dayOfWeek] ?: ""}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${day.dayType.labelPtBr}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$e1</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$s1</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$e2</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$s2</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$e3</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$s3</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$e4</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">$s4</Data></Cell>
    <Cell ss:StyleID="DataCellBold"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(day.workedMinutes)}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${if (day.overtime50Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime50Minutes) else "—"}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${if (day.overtime60Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime60Minutes) else "—"}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${if (day.overtime100Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime100Minutes) else "—"}</Data></Cell>
    <Cell ss:StyleID="DataCellCenter"><Data ss:Type="String">${if (day.nightMinutesReduced > 0) CalculationEngine.formatMinutesToHours(day.nightMinutesReduced) else "—"}</Data></Cell>
    <Cell ss:StyleID="DataCellLeft"><Data ss:Type="String">$obs</Data></Cell>
   </Row>
""")
            }

            // Summary Totals
            writer.write("""   <Row ss:Index="${summary.dayResults.size + 6}" ss:Height="20">
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">TOTAIS</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes)}</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)}</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)}</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)}</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String">${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)}</Data></Cell>
    <Cell ss:StyleID="TotalHeader"><Data ss:Type="String"></Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell><Data ss:Type="String">Remuneração Normal Estimada:</Data></Cell>
    <Cell><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents)}</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell><Data ss:Type="String">${if (summary.isWeekdayOtNormal) "Extras Dias Úteis (Valor Normal):" else "Extras 50%:"}</Data></Cell>
    <Cell><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents)}</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell><Data ss:Type="String">Extras 60% (Sábado):</Data></Cell>
    <Cell><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents)}</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell><Data ss:Type="String">Extras 100% (Dom/Fer/Folga):</Data></Cell>
    <Cell><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents)}</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell><Data ss:Type="String">Adicional Noturno:</Data></Cell>
    <Cell><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents)}</Data></Cell>
   </Row>
   <Row ss:Height="20">
    <Cell ss:StyleID="HeaderMeta"><Data ss:Type="String">TOTAL BRUTO ESTIMADO:</Data></Cell>
    <Cell ss:StyleID="HeaderMeta"><Data ss:Type="String">${CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents)}</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
</Workbook>
""")
        }

        return file
    }
}
