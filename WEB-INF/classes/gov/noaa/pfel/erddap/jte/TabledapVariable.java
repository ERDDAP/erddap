package gov.noaa.pfel.erddap.jte;

public class TabledapVariable {
  private final int index;
  private final String name;
  private final String labelHtml;
  private final boolean checked;
  private final String op0;
  private final String val0;
  private final String op1;
  private final String val1;
  private final String valTooltip;
  private final boolean hasDistinctOptions;
  private final String[] distinctOptions;
  private final int distinctSelected;
  private final String minStr;
  private final String maxStr;
  private final boolean hasSlider;
  private final String sliderHtml;

  public TabledapVariable(
      int index,
      String name,
      String labelHtml,
      boolean checked,
      String op0,
      String val0,
      String op1,
      String val1,
      String valTooltip,
      boolean hasDistinctOptions,
      String[] distinctOptions,
      int distinctSelected,
      String minStr,
      String maxStr,
      boolean hasSlider,
      String sliderHtml) {
    this.index = index;
    this.name = name;
    this.labelHtml = labelHtml;
    this.checked = checked;
    this.op0 = op0;
    this.val0 = val0;
    this.op1 = op1;
    this.val1 = val1;
    this.valTooltip = valTooltip;
    this.hasDistinctOptions = hasDistinctOptions;
    this.distinctOptions = distinctOptions;
    this.distinctSelected = distinctSelected;
    this.minStr = minStr;
    this.maxStr = maxStr;
    this.hasSlider = hasSlider;
    this.sliderHtml = sliderHtml;
  }

  public int getIndex() {
    return index;
  }

  public String getName() {
    return name;
  }

  public String getLabelHtml() {
    return labelHtml;
  }

  public boolean isChecked() {
    return checked;
  }

  public String getOp0() {
    return op0;
  }

  public String getVal0() {
    return val0;
  }

  public String getOp1() {
    return op1;
  }

  public String getVal1() {
    return val1;
  }

  public String getValTooltip() {
    return valTooltip;
  }

  public boolean isHasDistinctOptions() {
    return hasDistinctOptions;
  }

  public boolean hasDistinctOptions() {
    return hasDistinctOptions;
  }

  public String[] getDistinctOptions() {
    return distinctOptions;
  }

  public int getDistinctSelected() {
    return distinctSelected;
  }

  public String getMinStr() {
    return minStr;
  }

  public String getMaxStr() {
    return maxStr;
  }

  public boolean isHasSlider() {
    return hasSlider;
  }

  public boolean hasSlider() {
    return hasSlider;
  }

  public String getSliderHtml() {
    return sliderHtml;
  }
}
