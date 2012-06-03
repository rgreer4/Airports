/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

import com.nadmm.airports.donate.DonateActivity;

public class AboutActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.about_view );
        WebView webView = (WebView) findViewById( R.id.about_content );
        webView.loadUrl( "file:///android_asset/about.html" );

        Button btnDonate = (Button) findViewById( R.id.btn_donate );
        btnDonate.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick( View v ) {
                Intent intent = new Intent( AboutActivity.this, DonateActivity.class );
                startActivity( intent );
            }
        } );

        Button btnOk = (Button) findViewById( R.id.btn_close );
        btnOk.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                finish();
            }

        } );
    }

}
