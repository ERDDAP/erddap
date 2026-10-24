@import gov.noaa.pfel.erddap.jte.YouAreHere
@import gov.noaa.pfel.erddap.jte.TemplateHelper
@import gov.noaa.pfel.erddap.util.EDMessages.Message
@import com.cohort.util.String2
@import jakarta.servlet.http.HttpServletRequest
@import java.util.List

@param YouAreHere youAreHere
@param int language
@param String tErddapUrl
@param String loggedInAs = ""
@param String endOfRequest = "convert/color.html"
@param HttpServletRequest request = null
@param String queryValue = ""
@param String palette = "Rainbow"
@param String[] palettes = new String[0]
@param String continuity = "C"
@param String scale = "linear"
@param List<String> scales = List.of()
@param String minStr = "0"
@param String maxStr = "20"
@param String nSectionsStr = "10"
@param String hexColor = ""
@param String rgbColor = ""
@param String colorMapRender = ""
@param String tError = null

@template.layout.main(
    tErddapUrl=tErddapUrl,
    language=language,
    youAreHere=youAreHere,
    endOfRequest=endOfRequest,
    loggedInAs=loggedInAs,
    request=request,
    content=@`
<div class="standard_width">
    <h2>${TemplateHelper.getMessage(Message.CONVERT_COLORS_MESSAGE, language)}</h2>

    <form method="GET" action="${tErddapUrl}/convert/color.html" name="getColor">
        <b>Value to convert</b> (e.g., 12.5):
        <input type="text" name="value" value="${queryValue}" size="10" maxlength="15" title="Enter a number">
        <br><br>

        <div><b>Define the Colorbar:</b></div>
        Color Palette:
        <select name="p" size="1" title="Select the base color palette to use">
            @for(var pName : palettes)
                <option value="${pName}" selected="${pName.equalsIgnoreCase(palette)}">${pName}</option>
            @endfor
        </select>
        <br>

        Continuity:
        <select name="pc" size="1" title="Continuous or Discrete">
            <option value="Continuous" selected="${!"Discrete".equalsIgnoreCase(continuity)}">Continuous</option>
            <option value="Discrete" selected="${"Discrete".equalsIgnoreCase(continuity)}">Discrete</option>
        </select>
        <br>

        Scale:
        <select name="ps" size="1" title="Scale">
            @for(var sName : scales)
                <option value="${sName}" selected="${sName.equalsIgnoreCase(scale)}">${sName}</option>
            @endfor
        </select>
        <br>

        Colorbar Minimum Value:
        <input type="text" name="pMin" value="${minStr}" size="10" maxlength="60" title="Minimum value represented by the colorbar">
        <br>

        Colorbar Maximum Value:
        <input type="text" name="pMax" value="${maxStr}" size="10" maxlength="60" title="Maximum value represented by the colorbar">
        <br>

        Colorbar Sections:
        <input type="text" name="pSec" value="${nSectionsStr}" size="10" maxlength="60" title="Number of sections in the color bar">
        <br>

        <button type="submit" value="Get Color">Get Color</button>

        @if(String2.isSomething(queryValue))
            @if(tError == null)
                <br><br><div><b>Value color:</b></div>
                <span>Hex: ${hexColor}</span>
                <br><span>RGB: ${rgbColor}</span>
                <br><div style="height:15px; width:40px; background-color:${hexColor}; border:1px solid black;"></div><br>
                @if(String2.isSomething(colorMapRender))
                    $unsafe{colorMapRender}
                @endif
            @else
                <br><span class="warningColor">${tError}</span>
            @endif
        @endif
        <br>
    </form>
</div>
`
)
