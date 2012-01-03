/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.nadmm.airports.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.wx.Metar;
import com.nadmm.airports.wx.SkyCondition;

public class WxUtils {

    static public int getFlightCategoryColor( String flightCategory ) {
        int color = 0;
        if ( flightCategory.equals( "VFR" ) ) {
            color = Color.argb( 224, 80, 176, 96 );
        } else if ( flightCategory.equals( "MVFR" ) ) {
            color = Color.argb( 224, 48, 128, 224 );
        } else if ( flightCategory.equals( "IFR" ) ) {
            color = Color.argb( 224, 240, 32, 16 );
        } else if ( flightCategory.equals( "LIFR" ) ) {
            color = Color.argb( 224, 240, 48, 128 );
        }
        return color;
    }

    static public Drawable getColorizedDrawable( Resources res, String flightCategory,
            int resid ) {
        Drawable d = res.getDrawable( resid ).mutate();
        int color = getFlightCategoryColor( flightCategory );
        d.setColorFilter( color, PorterDuff.Mode.SRC_ATOP );
        return d;
    }

    static public void showColorizedDrawable( TextView tv, String flightCategory, int resid ) {
        // Get a mutable copy of the drawable so each can be set to a different color
        Resources res = tv.getResources();
        Drawable d = getColorizedDrawable( res, flightCategory, resid );
        GuiUtils.setTextViewDrawable( tv, d );
    }

    static public void setFlightCategoryDrawable( TextView tv, Metar metar ) {
        showColorizedDrawable( tv, metar.flightCategory, R.drawable.circle );
    }

    static public void setColorizedCeilingDrawable( TextView tv, Metar metar ) {
        SkyCondition sky = metar.skyConditions.get( metar.skyConditions.size()-1 );
        showColorizedDrawable( tv, metar.flightCategory, sky.getDrawable() );
    }

    static public Drawable getWindBarbDrawable( Context context, Metar metar ) {
        Drawable d = null;
        if ( metar.windDirDegrees > 0 && metar.windSpeedKnots > 0 ) {
            int resid = 0;
            if ( metar.windSpeedKnots >= 48 ) {
                resid = R.drawable.windbarb50;
            } else if ( metar.windSpeedKnots >= 43 ) {
                resid = R.drawable.windbarb45;
            } else if ( metar.windSpeedKnots >= 38 ) {
                resid = R.drawable.windbarb40;
            } else if ( metar.windSpeedKnots >= 33 ) {
                resid = R.drawable.windbarb35;
            } else if ( metar.windSpeedKnots >= 28 ) {
                resid = R.drawable.windbarb30;
            } else if ( metar.windSpeedKnots >= 23 ) {
                resid = R.drawable.windbarb25;
            } else if ( metar.windSpeedKnots >= 18 ) {
                resid = R.drawable.windbarb20;
            } else if ( metar.windSpeedKnots >= 13 ) {
                resid = R.drawable.windbarb15;
            } else if ( metar.windSpeedKnots >= 8 ) {
                resid = R.drawable.windbarb10;
            } else if ( metar.windSpeedKnots >= 3 ) {
                resid = R.drawable.windbarb5;
            } else {
                resid = R.drawable.windbarb0;
            }
            d = GuiUtils.getRotatedDrawable( context, resid, metar.windDirDegrees );
        }
        return d;
    }

    static public void setColorizedWxDrawable( TextView tv, Metar metar ) {
        Resources res = tv.getResources();
        SkyCondition sky = metar.skyConditions.get( metar.skyConditions.size()-1 );
        Drawable d1 = getColorizedDrawable( res, metar.flightCategory, sky.getDrawable() );
        Drawable d2 = null;
        if ( metar.windDirDegrees > 0 && metar.windDirDegrees < Integer.MAX_VALUE
                && metar.windSpeedKnots > 0 && metar.windSpeedKnots < Integer.MAX_VALUE ) {
            d2 = getWindBarbDrawable( tv.getContext(), metar );
        }
        Drawable result = GuiUtils.combineDrawables( d1, d2 );
        GuiUtils.setTextViewDrawable( tv, result );
    }

    static public float celsiusToFahrenheit( float tempCelsius ) {
        return ( tempCelsius*9/5 )+32;
    }

    static public float fahrenheitToCelsius( float tempFahrenheit ) {
        return ( tempFahrenheit-32 )*5/9;
    }

    static public float celsiusToKelvin( float tempCelsius ) {
        return (float) ( tempCelsius+273.15 );
    }

    static public float kelvinToCelsius( float tempKelvin ) {
        return (float) ( tempKelvin-273.15 );
    }

    static public float kelvinToRankine( float tempKelvin ) {
        return (float) ( celsiusToFahrenheit( kelvinToCelsius( tempKelvin ) )+459.69 );
    }

    static public float hgToMillibar( float altimHg ) {
        return (float) ( 33.8639*altimHg );
    }

    static public float getVirtualTemperature( float tempCelsius, float dewpointCelsius,
            float stationPressureHg ) {
        float tempKelvin = celsiusToKelvin( tempCelsius );
        float eMb = getVaporPressure( dewpointCelsius );
        float pMb = hgToMillibar( stationPressureHg );
        return (float) ( tempKelvin/( 1-( eMb/pMb )*( 1-0.622 ) ) );
    }

    static public float getVaporPressure( float tempCelsius ) {
        // Vapor pressure is in mb or hPa
        return (float) ( 6.1094*Math.exp( ( 17.625*tempCelsius )/( tempCelsius+243.04 ) ) );
    }

    static public float getStationPressure( Float altimHg, float elevMeters ) {
        // Station pressure is in inHg
        return (float) ( altimHg*Math.pow( ( 288-0.0065*elevMeters )/288, 5.2561 ) );
    }

    static public float getRelativeHumidity( Metar metar ) {
        return getRelativeHumidity( metar.tempCelsius, metar.dewpointCelsius );
    }

    static public float getRelativeHumidity( float tempCelsius, float dewpointCelsius ) {
        float e = getVaporPressure( dewpointCelsius );
        float es = getVaporPressure( tempCelsius );
        return (e/es)*100;
    }

    static public long getPressureAltitude( Metar metar ) {
        return getPressureAltitude( metar.altimeterHg, metar.stationElevationMeters );
    }

    static public long getPressureAltitude( float altimHg, float elevMeters ) {
        float pMb = hgToMillibar( getStationPressure( altimHg, elevMeters ) );
        return Math.round( ( 1-Math.pow( pMb/1013.25, 0.190284 ) )*145366.45 );
    }

    static public long getDensityAltitude( Metar metar ) {
        return getDensityAltitude( metar.tempCelsius, metar.dewpointCelsius, metar.altimeterHg,
                metar.stationElevationMeters );
    }

    static public long getDensityAltitude( float tempCelsius, float dewpointCelsius,
            float altimHg, float elevMeters ) {
        float stationPressureHg = getStationPressure( altimHg, elevMeters );
        float tvKelvin = getVirtualTemperature( tempCelsius, dewpointCelsius, stationPressureHg );
        float tvRankine = kelvinToRankine( tvKelvin );
        return Math.round( 145366*( 1-Math.pow( 17.326*stationPressureHg/tvRankine, 0.235 ) ) );
    }

}