import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb

/**
 * # ColorBrewer2
 *
 * https://colorbrewer2.org/
 *
 * Based on the research of Dr. Cynthia Brewer.
 */

enum class ColorBrewer2Type {
    Any, Diverging, Qualitative, Sequential
}

class ColorBrewer2Palette(val colors: List<ColorRGBa>, val type: ColorBrewer2Type)

val colorBrewer2 = listOf(
    ColorBrewer2Palette(
        listOf(
            rgb(0.9882, 0.5529, 0.3490),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.6000, 0.8353, 0.5804)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.1686, 0.5137, 0.7294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.1686, 0.5137, 0.7294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6000, 0.8353, 0.5804),
            rgb(0.1961, 0.5333, 0.7412)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6000, 0.8353, 0.5804),
            rgb(0.1961, 0.5333, 0.7412)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.1961, 0.5333, 0.7412)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.1961, 0.5333, 0.7412)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6196, 0.0039, 0.2588),
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.1961, 0.5333, 0.7412),
            rgb(0.3686, 0.3098, 0.6353)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6196, 0.0039, 0.2588),
            rgb(0.8353, 0.2431, 0.3098),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.9020, 0.9608, 0.5961),
            rgb(0.6706, 0.8667, 0.6431),
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.1961, 0.5333, 0.7412),
            rgb(0.3686, 0.3098, 0.6353)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9882, 0.5529, 0.3490),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.5686, 0.8118, 0.3765)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.1020, 0.5882, 0.2549)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.1020, 0.5882, 0.2549)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.5686, 0.8118, 0.3765),
            rgb(0.1020, 0.5961, 0.3137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.5686, 0.8118, 0.3765),
            rgb(0.1020, 0.5961, 0.3137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.4000, 0.7412, 0.3882),
            rgb(0.1020, 0.5961, 0.3137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.4000, 0.7412, 0.3882),
            rgb(0.1020, 0.5961, 0.3137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6471, 0.0000, 0.1490),
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.4000, 0.7412, 0.3882),
            rgb(0.1020, 0.5961, 0.3137),
            rgb(0.0000, 0.4078, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6471, 0.0000, 0.1490),
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5451),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8510, 0.9373, 0.5451),
            rgb(0.6510, 0.8510, 0.4157),
            rgb(0.4000, 0.7412, 0.3882),
            rgb(0.1020, 0.5961, 0.3137),
            rgb(0.0000, 0.4078, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.5412, 0.3843),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.4039, 0.6627, 0.8118)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7922, 0.0000, 0.1255),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.0196, 0.4431, 0.6902)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7922, 0.0000, 0.1255),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.0196, 0.4431, 0.6902)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.9373, 0.5412, 0.3843),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.1294, 0.4000, 0.6745)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.9373, 0.5412, 0.3843),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.1294, 0.4000, 0.6745)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.2627, 0.5765, 0.7647),
            rgb(0.1294, 0.4000, 0.6745)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.2627, 0.5765, 0.7647),
            rgb(0.1294, 0.4000, 0.6745)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4039, 0.0000, 0.1216),
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.2627, 0.5765, 0.7647),
            rgb(0.1294, 0.4000, 0.6745),
            rgb(0.0196, 0.1882, 0.3804)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4039, 0.0000, 0.1216),
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8196, 0.8980, 0.9412),
            rgb(0.5725, 0.7725, 0.8706),
            rgb(0.2627, 0.5765, 0.7647),
            rgb(0.1294, 0.4000, 0.6745),
            rgb(0.0196, 0.1882, 0.3804)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9137, 0.6392, 0.7882),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.6314, 0.8431, 0.4157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8157, 0.1098, 0.5451),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.3020, 0.6745, 0.1490)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8157, 0.1098, 0.5451),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.3020, 0.6745, 0.1490)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.9137, 0.6392, 0.7882),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.6314, 0.8431, 0.4157),
            rgb(0.3020, 0.5725, 0.1294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.9137, 0.6392, 0.7882),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.6314, 0.8431, 0.4157),
            rgb(0.3020, 0.5725, 0.1294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.8706, 0.4667, 0.6824),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.4980, 0.7373, 0.2549),
            rgb(0.3020, 0.5725, 0.1294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.8706, 0.4667, 0.6824),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.4980, 0.7373, 0.2549),
            rgb(0.3020, 0.5725, 0.1294)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5569, 0.0039, 0.3216),
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.8706, 0.4667, 0.6824),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.4980, 0.7373, 0.2549),
            rgb(0.3020, 0.5725, 0.1294),
            rgb(0.1529, 0.3922, 0.0980)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5569, 0.0039, 0.3216),
            rgb(0.7725, 0.1059, 0.4902),
            rgb(0.8706, 0.4667, 0.6824),
            rgb(0.9451, 0.7137, 0.8549),
            rgb(0.9922, 0.8784, 0.9373),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.9020, 0.9608, 0.8157),
            rgb(0.7216, 0.8824, 0.5255),
            rgb(0.4980, 0.7373, 0.2549),
            rgb(0.3020, 0.5725, 0.1294),
            rgb(0.1529, 0.3922, 0.0980)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6863, 0.5529, 0.7647),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.4980, 0.7490, 0.4824)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4824, 0.1961, 0.5804),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.0000, 0.5333, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4824, 0.1961, 0.5804),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.0000, 0.5333, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6863, 0.5529, 0.7647),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.4980, 0.7490, 0.4824),
            rgb(0.1059, 0.4706, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6863, 0.5529, 0.7647),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.4980, 0.7490, 0.4824),
            rgb(0.1059, 0.4706, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6000, 0.4392, 0.6706),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.3529, 0.6824, 0.3804),
            rgb(0.1059, 0.4706, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6000, 0.4392, 0.6706),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.3529, 0.6824, 0.3804),
            rgb(0.1059, 0.4706, 0.2157)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.2510, 0.0000, 0.2941),
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6000, 0.4392, 0.6706),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.3529, 0.6824, 0.3804),
            rgb(0.1059, 0.4706, 0.2157),
            rgb(0.0000, 0.2667, 0.1059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.2510, 0.0000, 0.2941),
            rgb(0.4627, 0.1647, 0.5137),
            rgb(0.6000, 0.4392, 0.6706),
            rgb(0.7608, 0.6471, 0.8118),
            rgb(0.9059, 0.8314, 0.9098),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8510, 0.9412, 0.8275),
            rgb(0.6510, 0.8588, 0.6275),
            rgb(0.3529, 0.6824, 0.3804),
            rgb(0.1059, 0.4706, 0.2157),
            rgb(0.0000, 0.2667, 0.1059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9882, 0.5529, 0.3490),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.5686, 0.7490, 0.8588)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.1725, 0.4824, 0.7137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.0980, 0.1098),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.1725, 0.4824, 0.7137)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.5686, 0.7490, 0.8588),
            rgb(0.2706, 0.4588, 0.7059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.5686, 0.7490, 0.8588),
            rgb(0.2706, 0.4588, 0.7059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.4549, 0.6784, 0.8196),
            rgb(0.2706, 0.4588, 0.7059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.4549, 0.6784, 0.8196),
            rgb(0.2706, 0.4588, 0.7059)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6471, 0.0000, 0.1490),
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.4549, 0.6784, 0.8196),
            rgb(0.2706, 0.4588, 0.7059),
            rgb(0.1922, 0.2118, 0.5843)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6471, 0.0000, 0.1490),
            rgb(0.8431, 0.1882, 0.1529),
            rgb(0.9569, 0.4275, 0.2627),
            rgb(0.9922, 0.6824, 0.3804),
            rgb(0.9961, 0.8784, 0.5647),
            rgb(1.0000, 1.0000, 0.7490),
            rgb(0.8784, 0.9529, 0.9725),
            rgb(0.6706, 0.8510, 0.9137),
            rgb(0.4549, 0.6784, 0.8196),
            rgb(0.2706, 0.4588, 0.7059),
            rgb(0.1922, 0.2118, 0.5843)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8471, 0.7020, 0.3961),
            rgb(0.9608, 0.9608, 0.9608),
            rgb(0.3529, 0.7059, 0.6745)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.3804, 0.1020),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.0039, 0.5216, 0.4431)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.3804, 0.1020),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.9608, 0.9608, 0.9608),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.0039, 0.5216, 0.4431)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.8471, 0.7020, 0.3961),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.3529, 0.7059, 0.6745),
            rgb(0.0039, 0.4000, 0.3686)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.8471, 0.7020, 0.3961),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.9608, 0.9608, 0.9608),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.3529, 0.7059, 0.6745),
            rgb(0.0039, 0.4000, 0.3686)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.7490, 0.5059, 0.1765),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.2078, 0.5922, 0.5608),
            rgb(0.0039, 0.4000, 0.3686)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.7490, 0.5059, 0.1765),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.9608, 0.9608, 0.9608),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.2078, 0.5922, 0.5608),
            rgb(0.0039, 0.4000, 0.3686)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.3294, 0.1882, 0.0196),
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.7490, 0.5059, 0.1765),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.2078, 0.5922, 0.5608),
            rgb(0.0039, 0.4000, 0.3686),
            rgb(0.0000, 0.2353, 0.1882)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.3294, 0.1882, 0.0196),
            rgb(0.5490, 0.3176, 0.0392),
            rgb(0.7490, 0.5059, 0.1765),
            rgb(0.8745, 0.7608, 0.4902),
            rgb(0.9647, 0.9098, 0.7647),
            rgb(0.9608, 0.9608, 0.9608),
            rgb(0.7804, 0.9176, 0.8980),
            rgb(0.5020, 0.8039, 0.7569),
            rgb(0.2078, 0.5922, 0.5608),
            rgb(0.0039, 0.4000, 0.3686),
            rgb(0.0000, 0.2353, 0.1882)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.5412, 0.3843),
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.6000, 0.6000, 0.6000)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7922, 0.0000, 0.1255),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.2510, 0.2510, 0.2510)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7922, 0.0000, 0.1255),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.2510, 0.2510, 0.2510)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.9373, 0.5412, 0.3843),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.6000, 0.6000, 0.6000),
            rgb(0.3020, 0.3020, 0.3020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.9373, 0.5412, 0.3843),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.6000, 0.6000, 0.6000),
            rgb(0.3020, 0.3020, 0.3020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.5294, 0.5294, 0.5294),
            rgb(0.3020, 0.3020, 0.3020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.5294, 0.5294, 0.5294),
            rgb(0.3020, 0.3020, 0.3020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4039, 0.0000, 0.1216),
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.5294, 0.5294, 0.5294),
            rgb(0.3020, 0.3020, 0.3020),
            rgb(0.1020, 0.1020, 0.1020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4039, 0.0000, 0.1216),
            rgb(0.6980, 0.0941, 0.1686),
            rgb(0.8392, 0.3765, 0.3020),
            rgb(0.9569, 0.6471, 0.5098),
            rgb(0.9922, 0.8588, 0.7804),
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.8784, 0.8784, 0.8784),
            rgb(0.7294, 0.7294, 0.7294),
            rgb(0.5294, 0.5294, 0.5294),
            rgb(0.3020, 0.3020, 0.3020),
            rgb(0.1020, 0.1020, 0.1020)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.6392, 0.2510),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.6000, 0.5569, 0.7647)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9020, 0.3804, 0.0039),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.3686, 0.2353, 0.6000)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9020, 0.3804, 0.0039),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.3686, 0.2353, 0.6000)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.9451, 0.6392, 0.2510),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6000, 0.5569, 0.7647),
            rgb(0.3294, 0.1529, 0.5333)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.9451, 0.6392, 0.2510),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6000, 0.5569, 0.7647),
            rgb(0.3294, 0.1529, 0.5333)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.8784, 0.5098, 0.0784),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.5020, 0.4510, 0.6745),
            rgb(0.3294, 0.1529, 0.5333)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.8784, 0.5098, 0.0784),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.5020, 0.4510, 0.6745),
            rgb(0.3294, 0.1529, 0.5333)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.2314, 0.0314),
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.8784, 0.5098, 0.0784),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.5020, 0.4510, 0.6745),
            rgb(0.3294, 0.1529, 0.5333),
            rgb(0.1765, 0.0000, 0.2941)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.2314, 0.0314),
            rgb(0.7020, 0.3451, 0.0235),
            rgb(0.8784, 0.5098, 0.0784),
            rgb(0.9922, 0.7216, 0.3882),
            rgb(0.9961, 0.8784, 0.7137),
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8471, 0.8549, 0.9216),
            rgb(0.6980, 0.6706, 0.8235),
            rgb(0.5020, 0.4510, 0.6745),
            rgb(0.3294, 0.1529, 0.5333),
            rgb(0.1765, 0.0000, 0.2941)
        ), ColorBrewer2Type.Diverging
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961),
            rgb(0.9059, 0.5412, 0.7647)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961),
            rgb(0.9059, 0.5412, 0.7647),
            rgb(0.6510, 0.8471, 0.3294)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961),
            rgb(0.9059, 0.5412, 0.7647),
            rgb(0.6510, 0.8471, 0.3294),
            rgb(1.0000, 0.8510, 0.1843)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961),
            rgb(0.9059, 0.5412, 0.7647),
            rgb(0.6510, 0.8471, 0.3294),
            rgb(1.0000, 0.8510, 0.1843),
            rgb(0.8980, 0.7686, 0.5804)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4000, 0.7608, 0.6471),
            rgb(0.9882, 0.5529, 0.3843),
            rgb(0.5529, 0.6275, 0.7961),
            rgb(0.9059, 0.5412, 0.7647),
            rgb(0.6510, 0.8471, 0.3294),
            rgb(1.0000, 0.8510, 0.1843),
            rgb(0.8980, 0.7686, 0.5804),
            rgb(0.7020, 0.7020, 0.7020)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255),
            rgb(1.0000, 1.0000, 0.6000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255),
            rgb(1.0000, 1.0000, 0.6000),
            rgb(0.2196, 0.4235, 0.6902)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255),
            rgb(1.0000, 1.0000, 0.6000),
            rgb(0.2196, 0.4235, 0.6902),
            rgb(0.9412, 0.0078, 0.4980)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255),
            rgb(1.0000, 1.0000, 0.6000),
            rgb(0.2196, 0.4235, 0.6902),
            rgb(0.9412, 0.0078, 0.4980),
            rgb(0.7490, 0.3569, 0.0902)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.4980, 0.7882, 0.4980),
            rgb(0.7451, 0.6824, 0.8314),
            rgb(0.9922, 0.7529, 0.5255),
            rgb(1.0000, 1.0000, 0.6000),
            rgb(0.2196, 0.4235, 0.6902),
            rgb(0.9412, 0.0078, 0.4980),
            rgb(0.7490, 0.3569, 0.0902),
            rgb(0.4000, 0.4000, 0.4000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392),
            rgb(1.0000, 0.4980, 0.0000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(1.0000, 1.0000, 0.2000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(1.0000, 1.0000, 0.2000),
            rgb(0.6510, 0.3373, 0.1569)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(1.0000, 1.0000, 0.2000),
            rgb(0.6510, 0.3373, 0.1569),
            rgb(0.9686, 0.5059, 0.7490)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8941, 0.1020, 0.1098),
            rgb(0.2157, 0.4941, 0.7216),
            rgb(0.3020, 0.6863, 0.2902),
            rgb(0.5961, 0.3059, 0.6392),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(1.0000, 1.0000, 0.2000),
            rgb(0.6510, 0.3373, 0.1569),
            rgb(0.9686, 0.5059, 0.7490),
            rgb(0.6000, 0.6000, 0.6000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118),
            rgb(0.9882, 0.8039, 0.8980)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118),
            rgb(0.9882, 0.8039, 0.8980),
            rgb(0.8510, 0.8510, 0.8510)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118),
            rgb(0.9882, 0.8039, 0.8980),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7373, 0.5020, 0.7412)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118),
            rgb(0.9882, 0.8039, 0.8980),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7373, 0.5020, 0.7412),
            rgb(0.8000, 0.9216, 0.7725)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.5529, 0.8275, 0.7804),
            rgb(1.0000, 1.0000, 0.7020),
            rgb(0.7451, 0.7294, 0.8549),
            rgb(0.9843, 0.5020, 0.4471),
            rgb(0.5020, 0.6941, 0.8275),
            rgb(0.9922, 0.7059, 0.3843),
            rgb(0.7020, 0.8706, 0.4118),
            rgb(0.9882, 0.8039, 0.8980),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7373, 0.5020, 0.7412),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(1.0000, 0.9294, 0.4353)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020),
            rgb(0.9059, 0.1608, 0.5412)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.4000, 0.6510, 0.1176)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.4000, 0.6510, 0.1176),
            rgb(0.9020, 0.6706, 0.0078)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.4000, 0.6510, 0.1176),
            rgb(0.9020, 0.6706, 0.0078),
            rgb(0.6510, 0.4627, 0.1137)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.1059, 0.6196, 0.4667),
            rgb(0.8510, 0.3725, 0.0078),
            rgb(0.4588, 0.4392, 0.7020),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.4000, 0.6510, 0.1176),
            rgb(0.9020, 0.6706, 0.0078),
            rgb(0.6510, 0.4627, 0.1137),
            rgb(0.4000, 0.4000, 0.4000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353),
            rgb(1.0000, 0.4980, 0.0000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(0.7922, 0.6980, 0.8392)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(0.7922, 0.6980, 0.8392),
            rgb(0.4157, 0.2392, 0.6039)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(0.7922, 0.6980, 0.8392),
            rgb(0.4157, 0.2392, 0.6039),
            rgb(1.0000, 1.0000, 0.6000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.6510, 0.8078, 0.8902),
            rgb(0.1216, 0.4706, 0.7059),
            rgb(0.6980, 0.8745, 0.5412),
            rgb(0.2000, 0.6275, 0.1725),
            rgb(0.9843, 0.6039, 0.6000),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.9922, 0.7490, 0.4353),
            rgb(1.0000, 0.4980, 0.0000),
            rgb(0.7922, 0.6980, 0.8392),
            rgb(0.4157, 0.2392, 0.6039),
            rgb(1.0000, 1.0000, 0.6000),
            rgb(0.6941, 0.3490, 0.1569)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098),
            rgb(0.9569, 0.7922, 0.8941)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098),
            rgb(0.9569, 0.7922, 0.8941),
            rgb(0.9020, 0.9608, 0.7882)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098),
            rgb(0.9569, 0.7922, 0.8941),
            rgb(0.9020, 0.9608, 0.7882),
            rgb(1.0000, 0.9490, 0.6824)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098),
            rgb(0.9569, 0.7922, 0.8941),
            rgb(0.9020, 0.9608, 0.7882),
            rgb(1.0000, 0.9490, 0.6824),
            rgb(0.9451, 0.8863, 0.8000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.7020, 0.8863, 0.8039),
            rgb(0.9922, 0.8039, 0.6745),
            rgb(0.7961, 0.8353, 0.9098),
            rgb(0.9569, 0.7922, 0.8941),
            rgb(0.9020, 0.9608, 0.7882),
            rgb(1.0000, 0.9490, 0.6824),
            rgb(0.9451, 0.8863, 0.8000),
            rgb(0.8000, 0.8000, 0.8000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941),
            rgb(0.9961, 0.8510, 0.6510)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941),
            rgb(0.9961, 0.8510, 0.6510),
            rgb(1.0000, 1.0000, 0.8000)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941),
            rgb(0.9961, 0.8510, 0.6510),
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.8980, 0.8471, 0.7412)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941),
            rgb(0.9961, 0.8510, 0.6510),
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.8980, 0.8471, 0.7412),
            rgb(0.9922, 0.8549, 0.9255)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9843, 0.7059, 0.6824),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.8706, 0.7961, 0.8941),
            rgb(0.9961, 0.8510, 0.6510),
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.8980, 0.8471, 0.7412),
            rgb(0.9922, 0.8549, 0.9255),
            rgb(0.9490, 0.9490, 0.9490)
        ), ColorBrewer2Type.Qualitative
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9098, 0.7843),
            rgb(0.9922, 0.7333, 0.5176),
            rgb(0.8902, 0.2902, 0.2000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9412, 0.8510),
            rgb(0.9922, 0.8000, 0.5412),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.8431, 0.1882, 0.1216)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9412, 0.8510),
            rgb(0.9922, 0.8000, 0.5412),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.8902, 0.2902, 0.2000),
            rgb(0.7020, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9412, 0.8510),
            rgb(0.9922, 0.8314, 0.6196),
            rgb(0.9922, 0.7333, 0.5176),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.8902, 0.2902, 0.2000),
            rgb(0.7020, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9412, 0.8510),
            rgb(0.9922, 0.8314, 0.6196),
            rgb(0.9922, 0.7333, 0.5176),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9373, 0.3961, 0.2824),
            rgb(0.8431, 0.1882, 0.1216),
            rgb(0.6000, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9255),
            rgb(0.9961, 0.9098, 0.7843),
            rgb(0.9922, 0.8314, 0.6196),
            rgb(0.9922, 0.7333, 0.5176),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9373, 0.3961, 0.2824),
            rgb(0.8431, 0.1882, 0.1216),
            rgb(0.6000, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9255),
            rgb(0.9961, 0.9098, 0.7843),
            rgb(0.9922, 0.8314, 0.6196),
            rgb(0.9922, 0.7333, 0.5176),
            rgb(0.9882, 0.5529, 0.3490),
            rgb(0.9373, 0.3961, 0.2824),
            rgb(0.8431, 0.1882, 0.1216),
            rgb(0.7020, 0.0000, 0.0000),
            rgb(0.4980, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9255, 0.9059, 0.9490),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.1686, 0.5490, 0.7451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.7412, 0.7882, 0.8824),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.0196, 0.4392, 0.6902)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.7412, 0.7882, 0.8824),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.1686, 0.5490, 0.7451),
            rgb(0.0157, 0.3529, 0.5529)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.1686, 0.5490, 0.7451),
            rgb(0.0157, 0.3529, 0.5529)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0196, 0.4392, 0.6902),
            rgb(0.0118, 0.3059, 0.4824)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9843),
            rgb(0.9255, 0.9059, 0.9490),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0196, 0.4392, 0.6902),
            rgb(0.0118, 0.3059, 0.4824)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9843),
            rgb(0.9255, 0.9059, 0.9490),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4549, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0196, 0.4392, 0.6902),
            rgb(0.0157, 0.3529, 0.5529),
            rgb(0.0078, 0.2196, 0.3451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8784, 0.9255, 0.9569),
            rgb(0.6196, 0.7373, 0.8549),
            rgb(0.5333, 0.3373, 0.6549)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5333, 0.2549, 0.6157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.7020, 0.8039, 0.8902),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5333, 0.3373, 0.6549),
            rgb(0.5059, 0.0588, 0.4863)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.7490, 0.8275, 0.9020),
            rgb(0.6196, 0.7373, 0.8549),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5333, 0.3373, 0.6549),
            rgb(0.5059, 0.0588, 0.4863)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.7490, 0.8275, 0.9020),
            rgb(0.6196, 0.7373, 0.8549),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5490, 0.4196, 0.6941),
            rgb(0.5333, 0.2549, 0.6157),
            rgb(0.4314, 0.0039, 0.4196)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9922),
            rgb(0.8784, 0.9255, 0.9569),
            rgb(0.7490, 0.8275, 0.9020),
            rgb(0.6196, 0.7373, 0.8549),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5490, 0.4196, 0.6941),
            rgb(0.5333, 0.2549, 0.6157),
            rgb(0.4314, 0.0039, 0.4196)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9922),
            rgb(0.8784, 0.9255, 0.9569),
            rgb(0.7490, 0.8275, 0.9020),
            rgb(0.6196, 0.7373, 0.8549),
            rgb(0.5490, 0.5882, 0.7765),
            rgb(0.5490, 0.4196, 0.6941),
            rgb(0.5333, 0.2549, 0.6157),
            rgb(0.5059, 0.0588, 0.4863),
            rgb(0.3020, 0.0000, 0.2941)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9020, 0.8078),
            rgb(0.9922, 0.6824, 0.4196),
            rgb(0.9020, 0.3333, 0.0510)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9294, 0.8706),
            rgb(0.9922, 0.7451, 0.5216),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.8510, 0.2784, 0.0039)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9294, 0.8706),
            rgb(0.9922, 0.7451, 0.5216),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9020, 0.3333, 0.0510),
            rgb(0.6510, 0.2118, 0.0118)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9294, 0.8706),
            rgb(0.9922, 0.8157, 0.6353),
            rgb(0.9922, 0.6824, 0.4196),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9020, 0.3333, 0.0510),
            rgb(0.6510, 0.2118, 0.0118)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9294, 0.8706),
            rgb(0.9922, 0.8157, 0.6353),
            rgb(0.9922, 0.6824, 0.4196),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9451, 0.4118, 0.0745),
            rgb(0.8510, 0.2824, 0.0039),
            rgb(0.5490, 0.1765, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9608, 0.9216),
            rgb(0.9961, 0.9020, 0.8078),
            rgb(0.9922, 0.8157, 0.6353),
            rgb(0.9922, 0.6824, 0.4196),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9451, 0.4118, 0.0745),
            rgb(0.8510, 0.2824, 0.0039),
            rgb(0.5490, 0.1765, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9608, 0.9216),
            rgb(0.9961, 0.9020, 0.8078),
            rgb(0.9922, 0.8157, 0.6353),
            rgb(0.9922, 0.6824, 0.4196),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9451, 0.4118, 0.0745),
            rgb(0.8510, 0.2824, 0.0039),
            rgb(0.6510, 0.2118, 0.0118),
            rgb(0.4980, 0.1529, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8980, 0.9608, 0.9765),
            rgb(0.6000, 0.8471, 0.7882),
            rgb(0.1725, 0.6353, 0.3725)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.6980, 0.8863, 0.8863),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.1373, 0.5451, 0.2706)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.6980, 0.8863, 0.8863),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.1725, 0.6353, 0.3725),
            rgb(0.0000, 0.4275, 0.1725)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.8000, 0.9255, 0.9020),
            rgb(0.6000, 0.8471, 0.7882),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.1725, 0.6353, 0.3725),
            rgb(0.0000, 0.4275, 0.1725)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9843),
            rgb(0.8000, 0.9255, 0.9020),
            rgb(0.6000, 0.8471, 0.7882),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.2549, 0.6824, 0.4627),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.3451, 0.1412)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9922),
            rgb(0.8980, 0.9608, 0.9765),
            rgb(0.8000, 0.9255, 0.9020),
            rgb(0.6000, 0.8471, 0.7882),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.2549, 0.6824, 0.4627),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.3451, 0.1412)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9922),
            rgb(0.8980, 0.9608, 0.9765),
            rgb(0.8000, 0.9255, 0.9020),
            rgb(0.6000, 0.8471, 0.7882),
            rgb(0.4000, 0.7608, 0.6431),
            rgb(0.2549, 0.6824, 0.4627),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.4275, 0.1725),
            rgb(0.0000, 0.2667, 0.1059)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.7373),
            rgb(0.9961, 0.7686, 0.3098),
            rgb(0.8510, 0.3725, 0.0549)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8314),
            rgb(0.9961, 0.8510, 0.5569),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.8000, 0.2980, 0.0078)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8314),
            rgb(0.9961, 0.8510, 0.5569),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.8510, 0.3725, 0.0549),
            rgb(0.6000, 0.2039, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8314),
            rgb(0.9961, 0.8902, 0.5686),
            rgb(0.9961, 0.7686, 0.3098),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.8510, 0.3725, 0.0549),
            rgb(0.6000, 0.2039, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8314),
            rgb(0.9961, 0.8902, 0.5686),
            rgb(0.9961, 0.7686, 0.3098),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.9255, 0.4392, 0.0784),
            rgb(0.8000, 0.2980, 0.0078),
            rgb(0.5490, 0.1765, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8980),
            rgb(1.0000, 0.9686, 0.7373),
            rgb(0.9961, 0.8902, 0.5686),
            rgb(0.9961, 0.7686, 0.3098),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.9255, 0.4392, 0.0784),
            rgb(0.8000, 0.2980, 0.0078),
            rgb(0.5490, 0.1765, 0.0157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8980),
            rgb(1.0000, 0.9686, 0.7373),
            rgb(0.9961, 0.8902, 0.5686),
            rgb(0.9961, 0.7686, 0.3098),
            rgb(0.9961, 0.6000, 0.1608),
            rgb(0.9255, 0.4392, 0.0784),
            rgb(0.8000, 0.2980, 0.0078),
            rgb(0.6000, 0.2039, 0.0157),
            rgb(0.4000, 0.1451, 0.0235)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.7255),
            rgb(0.6784, 0.8667, 0.5569),
            rgb(0.1922, 0.6392, 0.3294)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.7608, 0.9020, 0.6000),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.1373, 0.5176, 0.2627)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.7608, 0.9020, 0.6000),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.1922, 0.6392, 0.3294),
            rgb(0.0000, 0.4078, 0.2157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.8510, 0.9412, 0.6392),
            rgb(0.6784, 0.8667, 0.5569),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.1922, 0.6392, 0.3294),
            rgb(0.0000, 0.4078, 0.2157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.8510, 0.9412, 0.6392),
            rgb(0.6784, 0.8667, 0.5569),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5176, 0.2627),
            rgb(0.0000, 0.3529, 0.1961)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8980),
            rgb(0.9686, 0.9882, 0.7255),
            rgb(0.8510, 0.9412, 0.6392),
            rgb(0.6784, 0.8667, 0.5569),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5176, 0.2627),
            rgb(0.0000, 0.3529, 0.1961)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8980),
            rgb(0.9686, 0.9882, 0.7255),
            rgb(0.8510, 0.9412, 0.6392),
            rgb(0.6784, 0.8667, 0.5569),
            rgb(0.4706, 0.7765, 0.4745),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5176, 0.2627),
            rgb(0.0000, 0.4078, 0.2157),
            rgb(0.0000, 0.2706, 0.1608)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.8784, 0.8235),
            rgb(0.9882, 0.5725, 0.4471),
            rgb(0.8706, 0.1765, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.8980, 0.8510),
            rgb(0.9882, 0.6824, 0.5686),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.7961, 0.0941, 0.1137)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.8980, 0.8510),
            rgb(0.9882, 0.6824, 0.5686),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.8706, 0.1765, 0.1490),
            rgb(0.6471, 0.0588, 0.0824)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.8980, 0.8510),
            rgb(0.9882, 0.7333, 0.6314),
            rgb(0.9882, 0.5725, 0.4471),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.8706, 0.1765, 0.1490),
            rgb(0.6471, 0.0588, 0.0824)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.8980, 0.8510),
            rgb(0.9882, 0.7333, 0.6314),
            rgb(0.9882, 0.5725, 0.4471),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.9373, 0.2314, 0.1725),
            rgb(0.7961, 0.0941, 0.1137),
            rgb(0.6000, 0.0000, 0.0510)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9608, 0.9412),
            rgb(0.9961, 0.8784, 0.8235),
            rgb(0.9882, 0.7333, 0.6314),
            rgb(0.9882, 0.5725, 0.4471),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.9373, 0.2314, 0.1725),
            rgb(0.7961, 0.0941, 0.1137),
            rgb(0.6000, 0.0000, 0.0510)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9608, 0.9412),
            rgb(0.9961, 0.8784, 0.8235),
            rgb(0.9882, 0.7333, 0.6314),
            rgb(0.9882, 0.5725, 0.4471),
            rgb(0.9843, 0.4157, 0.2902),
            rgb(0.9373, 0.2314, 0.1725),
            rgb(0.7961, 0.0941, 0.1137),
            rgb(0.6471, 0.0588, 0.0824),
            rgb(0.4039, 0.0000, 0.0510)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9922, 0.8784, 0.8667),
            rgb(0.9804, 0.6235, 0.7098),
            rgb(0.7725, 0.1059, 0.5412)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9216, 0.8863),
            rgb(0.9843, 0.7059, 0.7255),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.6824, 0.0039, 0.4941)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9216, 0.8863),
            rgb(0.9843, 0.7059, 0.7255),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.7725, 0.1059, 0.5412),
            rgb(0.4784, 0.0039, 0.4667)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9216, 0.8863),
            rgb(0.9882, 0.7725, 0.7529),
            rgb(0.9804, 0.6235, 0.7098),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.7725, 0.1059, 0.5412),
            rgb(0.4784, 0.0039, 0.4667)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9961, 0.9216, 0.8863),
            rgb(0.9882, 0.7725, 0.7529),
            rgb(0.9804, 0.6235, 0.7098),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.8667, 0.2039, 0.5922),
            rgb(0.6824, 0.0039, 0.4941),
            rgb(0.4784, 0.0039, 0.4667)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9529),
            rgb(0.9922, 0.8784, 0.8667),
            rgb(0.9882, 0.7725, 0.7529),
            rgb(0.9804, 0.6235, 0.7098),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.8667, 0.2039, 0.5922),
            rgb(0.6824, 0.0039, 0.4941),
            rgb(0.4784, 0.0039, 0.4667)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9529),
            rgb(0.9922, 0.8784, 0.8667),
            rgb(0.9882, 0.7725, 0.7529),
            rgb(0.9804, 0.6235, 0.7098),
            rgb(0.9686, 0.4078, 0.6314),
            rgb(0.8667, 0.2039, 0.5922),
            rgb(0.6824, 0.0039, 0.4941),
            rgb(0.4784, 0.0039, 0.4667),
            rgb(0.2863, 0.0000, 0.4157)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8980, 0.9608, 0.8784),
            rgb(0.6314, 0.8510, 0.6078),
            rgb(0.1922, 0.6392, 0.3294)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9137),
            rgb(0.7294, 0.8941, 0.7020),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.1373, 0.5451, 0.2706)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9137),
            rgb(0.7294, 0.8941, 0.7020),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.1922, 0.6392, 0.3294),
            rgb(0.0000, 0.4275, 0.1725)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9137),
            rgb(0.7804, 0.9137, 0.7529),
            rgb(0.6314, 0.8510, 0.6078),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.1922, 0.6392, 0.3294),
            rgb(0.0000, 0.4275, 0.1725)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.9137),
            rgb(0.7804, 0.9137, 0.7529),
            rgb(0.6314, 0.8510, 0.6078),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.3529, 0.1961)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9608),
            rgb(0.8980, 0.9608, 0.8784),
            rgb(0.7804, 0.9137, 0.7529),
            rgb(0.6314, 0.8510, 0.6078),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.3529, 0.1961)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9608),
            rgb(0.8980, 0.9608, 0.8784),
            rgb(0.7804, 0.9137, 0.7529),
            rgb(0.6314, 0.8510, 0.6078),
            rgb(0.4549, 0.7686, 0.4627),
            rgb(0.2549, 0.6706, 0.3647),
            rgb(0.1373, 0.5451, 0.2706),
            rgb(0.0000, 0.4275, 0.1725),
            rgb(0.0000, 0.2667, 0.1059)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9294, 0.9725, 0.6941),
            rgb(0.4980, 0.8039, 0.7333),
            rgb(0.1725, 0.4980, 0.7216)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.6314, 0.8549, 0.7059),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1333, 0.3686, 0.6588)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.6314, 0.8549, 0.7059),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1725, 0.4980, 0.7216),
            rgb(0.1451, 0.2039, 0.5804)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.7804, 0.9137, 0.7059),
            rgb(0.4980, 0.8039, 0.7333),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1725, 0.4980, 0.7216),
            rgb(0.1451, 0.2039, 0.5804)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(0.7804, 0.9137, 0.7059),
            rgb(0.4980, 0.8039, 0.7333),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1137, 0.5686, 0.7529),
            rgb(0.1333, 0.3686, 0.6588),
            rgb(0.0471, 0.1725, 0.5176)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8510),
            rgb(0.9294, 0.9725, 0.6941),
            rgb(0.7804, 0.9137, 0.7059),
            rgb(0.4980, 0.8039, 0.7333),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1137, 0.5686, 0.7529),
            rgb(0.1333, 0.3686, 0.6588),
            rgb(0.0471, 0.1725, 0.5176)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8510),
            rgb(0.9294, 0.9725, 0.6941),
            rgb(0.7804, 0.9137, 0.7059),
            rgb(0.4980, 0.8039, 0.7333),
            rgb(0.2549, 0.7137, 0.7686),
            rgb(0.1137, 0.5686, 0.7529),
            rgb(0.1333, 0.3686, 0.6588),
            rgb(0.1451, 0.2039, 0.5804),
            rgb(0.0314, 0.1137, 0.3451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.9294, 0.9608),
            rgb(0.7373, 0.7412, 0.8627),
            rgb(0.4588, 0.4196, 0.6941)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9490, 0.9412, 0.9686),
            rgb(0.7961, 0.7882, 0.8863),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.4157, 0.3176, 0.6392)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9490, 0.9412, 0.9686),
            rgb(0.7961, 0.7882, 0.8863),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.4588, 0.4196, 0.6941),
            rgb(0.3294, 0.1529, 0.5608)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9490, 0.9412, 0.9686),
            rgb(0.8549, 0.8549, 0.9216),
            rgb(0.7373, 0.7412, 0.8627),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.4588, 0.4196, 0.6941),
            rgb(0.3294, 0.1529, 0.5608)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9490, 0.9412, 0.9686),
            rgb(0.8549, 0.8549, 0.9216),
            rgb(0.7373, 0.7412, 0.8627),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.5020, 0.4902, 0.7294),
            rgb(0.4157, 0.3176, 0.6392),
            rgb(0.2902, 0.0784, 0.5255)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9882, 0.9843, 0.9922),
            rgb(0.9373, 0.9294, 0.9608),
            rgb(0.8549, 0.8549, 0.9216),
            rgb(0.7373, 0.7412, 0.8627),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.5020, 0.4902, 0.7294),
            rgb(0.4157, 0.3176, 0.6392),
            rgb(0.2902, 0.0784, 0.5255)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9882, 0.9843, 0.9922),
            rgb(0.9373, 0.9294, 0.9608),
            rgb(0.8549, 0.8549, 0.9216),
            rgb(0.7373, 0.7412, 0.8627),
            rgb(0.6196, 0.6039, 0.7843),
            rgb(0.5020, 0.4902, 0.7294),
            rgb(0.4157, 0.3176, 0.6392),
            rgb(0.3294, 0.1529, 0.5608),
            rgb(0.2471, 0.0000, 0.4902)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8784, 0.9529, 0.8588),
            rgb(0.6588, 0.8667, 0.7098),
            rgb(0.2627, 0.6353, 0.7922)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9412, 0.9765, 0.9098),
            rgb(0.7294, 0.8941, 0.7373),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.1686, 0.5490, 0.7451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9412, 0.9765, 0.9098),
            rgb(0.7294, 0.8941, 0.7373),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.2627, 0.6353, 0.7922),
            rgb(0.0314, 0.4078, 0.6745)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9412, 0.9765, 0.9098),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.6588, 0.8667, 0.7098),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.2627, 0.6353, 0.7922),
            rgb(0.0314, 0.4078, 0.6745)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9412, 0.9765, 0.9098),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.6588, 0.8667, 0.7098),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.3059, 0.7020, 0.8275),
            rgb(0.1686, 0.5490, 0.7451),
            rgb(0.0314, 0.3451, 0.6196)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9412),
            rgb(0.8784, 0.9529, 0.8588),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.6588, 0.8667, 0.7098),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.3059, 0.7020, 0.8275),
            rgb(0.1686, 0.5490, 0.7451),
            rgb(0.0314, 0.3451, 0.6196)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9882, 0.9412),
            rgb(0.8784, 0.9529, 0.8588),
            rgb(0.8000, 0.9216, 0.7725),
            rgb(0.6588, 0.8667, 0.7098),
            rgb(0.4824, 0.8000, 0.7686),
            rgb(0.3059, 0.7020, 0.8275),
            rgb(0.1686, 0.5490, 0.7451),
            rgb(0.0314, 0.4078, 0.6745),
            rgb(0.0314, 0.2510, 0.5059)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9412, 0.9412, 0.9412),
            rgb(0.7412, 0.7412, 0.7412),
            rgb(0.3882, 0.3882, 0.3882)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8000, 0.8000, 0.8000),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.3216, 0.3216, 0.3216)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8000, 0.8000, 0.8000),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.3882, 0.3882, 0.3882),
            rgb(0.1451, 0.1451, 0.1451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7412, 0.7412, 0.7412),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.3882, 0.3882, 0.3882),
            rgb(0.1451, 0.1451, 0.1451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9686, 0.9686),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7412, 0.7412, 0.7412),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.4510, 0.4510, 0.4510),
            rgb(0.3216, 0.3216, 0.3216),
            rgb(0.1451, 0.1451, 0.1451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.9412, 0.9412, 0.9412),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7412, 0.7412, 0.7412),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.4510, 0.4510, 0.4510),
            rgb(0.3216, 0.3216, 0.3216),
            rgb(0.1451, 0.1451, 0.1451)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 1.0000),
            rgb(0.9412, 0.9412, 0.9412),
            rgb(0.8510, 0.8510, 0.8510),
            rgb(0.7412, 0.7412, 0.7412),
            rgb(0.5882, 0.5882, 0.5882),
            rgb(0.4510, 0.4510, 0.4510),
            rgb(0.3216, 0.3216, 0.3216),
            rgb(0.1451, 0.1451, 0.1451),
            rgb(0.0000, 0.0000, 0.0000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9294, 0.6275),
            rgb(0.9961, 0.6980, 0.2980),
            rgb(0.9412, 0.2314, 0.1255)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.6980),
            rgb(0.9961, 0.8000, 0.3608),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.8902, 0.1020, 0.1098)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.6980),
            rgb(0.9961, 0.8000, 0.3608),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9412, 0.2314, 0.1255),
            rgb(0.7412, 0.0000, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.6980),
            rgb(0.9961, 0.8510, 0.4627),
            rgb(0.9961, 0.6980, 0.2980),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9412, 0.2314, 0.1255),
            rgb(0.7412, 0.0000, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.6980),
            rgb(0.9961, 0.8510, 0.4627),
            rgb(0.9961, 0.6980, 0.2980),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9882, 0.3059, 0.1647),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.6941, 0.0000, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(1.0000, 0.9294, 0.6275),
            rgb(0.9961, 0.8510, 0.4627),
            rgb(0.9961, 0.6980, 0.2980),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9882, 0.3059, 0.1647),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.6941, 0.0000, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 1.0000, 0.8000),
            rgb(1.0000, 0.9294, 0.6275),
            rgb(0.9961, 0.8510, 0.4627),
            rgb(0.9961, 0.6980, 0.2980),
            rgb(0.9922, 0.5529, 0.2353),
            rgb(0.9882, 0.3059, 0.1647),
            rgb(0.8902, 0.1020, 0.1098),
            rgb(0.7412, 0.0000, 0.1490),
            rgb(0.5020, 0.0000, 0.1490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9059, 0.8824, 0.9373),
            rgb(0.7882, 0.5804, 0.7804),
            rgb(0.8667, 0.1098, 0.4667)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8431, 0.7098, 0.8471),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.8078, 0.0706, 0.3373)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8431, 0.7098, 0.8471),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.8667, 0.1098, 0.4667),
            rgb(0.5961, 0.0000, 0.2627)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8314, 0.7255, 0.8549),
            rgb(0.7882, 0.5804, 0.7804),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.8667, 0.1098, 0.4667),
            rgb(0.5961, 0.0000, 0.2627)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9451, 0.9333, 0.9647),
            rgb(0.8314, 0.7255, 0.8549),
            rgb(0.7882, 0.5804, 0.7804),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.8078, 0.0706, 0.3373),
            rgb(0.5686, 0.0000, 0.2471)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9569, 0.9765),
            rgb(0.9059, 0.8824, 0.9373),
            rgb(0.8314, 0.7255, 0.8549),
            rgb(0.7882, 0.5804, 0.7804),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.8078, 0.0706, 0.3373),
            rgb(0.5686, 0.0000, 0.2471)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9569, 0.9765),
            rgb(0.9059, 0.8824, 0.9373),
            rgb(0.8314, 0.7255, 0.8549),
            rgb(0.7882, 0.5804, 0.7804),
            rgb(0.8745, 0.3961, 0.6902),
            rgb(0.9059, 0.1608, 0.5412),
            rgb(0.8078, 0.0706, 0.3373),
            rgb(0.5961, 0.0000, 0.2627),
            rgb(0.4039, 0.0000, 0.1216)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.8706, 0.9216, 0.9686),
            rgb(0.6196, 0.7922, 0.8824),
            rgb(0.1922, 0.5098, 0.7412)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.9529, 1.0000),
            rgb(0.7412, 0.8431, 0.9059),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.1294, 0.4431, 0.7098)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.9529, 1.0000),
            rgb(0.7412, 0.8431, 0.9059),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.1922, 0.5098, 0.7412),
            rgb(0.0314, 0.3176, 0.6118)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.9529, 1.0000),
            rgb(0.7765, 0.8588, 0.9373),
            rgb(0.6196, 0.7922, 0.8824),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.1922, 0.5098, 0.7412),
            rgb(0.0314, 0.3176, 0.6118)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9373, 0.9529, 1.0000),
            rgb(0.7765, 0.8588, 0.9373),
            rgb(0.6196, 0.7922, 0.8824),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.2588, 0.5725, 0.7765),
            rgb(0.1294, 0.4431, 0.7098),
            rgb(0.0314, 0.2706, 0.5804)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9843, 1.0000),
            rgb(0.8706, 0.9216, 0.9686),
            rgb(0.7765, 0.8588, 0.9373),
            rgb(0.6196, 0.7922, 0.8824),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.2588, 0.5725, 0.7765),
            rgb(0.1294, 0.4431, 0.7098),
            rgb(0.0314, 0.2706, 0.5804)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9686, 0.9843, 1.0000),
            rgb(0.8706, 0.9216, 0.9686),
            rgb(0.7765, 0.8588, 0.9373),
            rgb(0.6196, 0.7922, 0.8824),
            rgb(0.4196, 0.6824, 0.8392),
            rgb(0.2588, 0.5725, 0.7765),
            rgb(0.1294, 0.4431, 0.7098),
            rgb(0.0314, 0.3176, 0.6118),
            rgb(0.0314, 0.1882, 0.4196)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9255, 0.8863, 0.9412),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.1098, 0.5647, 0.6000)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9647, 0.9373, 0.9686),
            rgb(0.7412, 0.7882, 0.8824),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.0078, 0.5059, 0.5412)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9647, 0.9373, 0.9686),
            rgb(0.7412, 0.7882, 0.8824),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.1098, 0.5647, 0.6000),
            rgb(0.0039, 0.4235, 0.3490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9647, 0.9373, 0.9686),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.1098, 0.5647, 0.6000),
            rgb(0.0039, 0.4235, 0.3490)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(0.9647, 0.9373, 0.9686),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0078, 0.5059, 0.5412),
            rgb(0.0039, 0.3922, 0.3137)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9843),
            rgb(0.9255, 0.8863, 0.9412),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0078, 0.5059, 0.5412),
            rgb(0.0039, 0.3922, 0.3137)
        ), ColorBrewer2Type.Sequential
    ),
    ColorBrewer2Palette(
        listOf(
            rgb(1.0000, 0.9686, 0.9843),
            rgb(0.9255, 0.8863, 0.9412),
            rgb(0.8157, 0.8196, 0.9020),
            rgb(0.6510, 0.7412, 0.8588),
            rgb(0.4039, 0.6627, 0.8118),
            rgb(0.2118, 0.5647, 0.7529),
            rgb(0.0078, 0.5059, 0.5412),
            rgb(0.0039, 0.4235, 0.3490),
            rgb(0.0039, 0.2745, 0.2118)
        ), ColorBrewer2Type.Sequential
    )
)

fun colorBrewer2Palettes(
    numberOfColors: Int? = null,
    paletteType: ColorBrewer2Type = ColorBrewer2Type.Any
) = when {
    numberOfColors == null && paletteType == ColorBrewer2Type.Any -> colorBrewer2
    paletteType == ColorBrewer2Type.Any -> colorBrewer2.filter { it.colors.size == numberOfColors }
    else -> colorBrewer2.filter { it.type == paletteType }
}
