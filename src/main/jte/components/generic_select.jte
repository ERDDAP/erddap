@import com.cohort.util.XML
@param String name
@param String[] options
@param String selected
@param String onchange = null
@param int size = 1

<select name="${name}" size="${Math.min((options==null) ? 0 : options.length, size)}" onchange="${onchange != null && onchange.length() > 0 ? onchange : ""}">
  @for (int i = 0; i < (options==null?0:options.length); i++)
    <option value="${options[i]}" selected="${selected != null && options[i].equals(selected)}">${options[i]}</option>
  @endfor
</select>
