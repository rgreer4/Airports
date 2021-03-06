/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Aff3;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.AtcPhones;
import com.nadmm.airports.data.DatabaseManager.Tower1;
import com.nadmm.airports.data.DatabaseManager.Tower2;
import com.nadmm.airports.data.DatabaseManager.Tower3;
import com.nadmm.airports.data.DatabaseManager.Tower4;
import com.nadmm.airports.data.DatabaseManager.Tower6;
import com.nadmm.airports.data.DatabaseManager.Tower7;
import com.nadmm.airports.data.DatabaseManager.Tower9;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class CommunicationsFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.comm_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "Communications", "" );

        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new CommunicationsTask() ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        String icaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
        getActivityBase().faLogViewItem( "comms", icaoCode );

        showAirportTitle( apt );
        showAirportFrequencies( result );
        showAtcHours( result );
        showAtcPhones( result );
        showRemarks( result );

        setFragmentContentShown( true );
    }

    protected void showAirportFrequencies( Cursor[] result ) {
        String towerRadioCall = "";
        String apchRadioCall = "";
        String depRadioCall = "";
        Map<String, ArrayList<Pair<String, String>>> map = new TreeMap<>();

        Cursor apt = result[ 0 ];
        String ctaf = apt.getString( apt.getColumnIndex( Airports.CTAF_FREQ ) );
        if ( !ctaf.isEmpty() ) {
            addFrequencyToMap( map, "CTAF", ctaf, "" );
        }
        String unicom = apt.getString( apt.getColumnIndex( Airports.UNICOM_FREQS ) );
        if ( !unicom.isEmpty() ) {
            addFrequencyToMap( map, "Unicom", unicom, "" );
        }

        Cursor twr1 = result[ 1 ];
        if ( twr1.moveToFirst() ) {
            towerRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_TOWER ) );
            try {
                apchRadioCall = twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_APCH_PRIMARY ) );
            } catch ( Exception e ) {
                apchRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_APCH ) );
            }
            try {
                depRadioCall = twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_DEP_PRIMARY ) );
            } catch ( Exception e ) {
                depRadioCall = twr1.getString( twr1.getColumnIndex( Tower1.RADIO_CALL_DEP ) );
            }
        }

        Cursor twr3 = result[ 2 ];
        if ( twr3.moveToFirst() ) {
            do {
                String freqUse = twr3.getString( twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ_USE ) );
                String freq = twr3.getString( twr3.getColumnIndex( Tower3.MASTER_AIRPORT_FREQ ) );
                processFrequency( map, towerRadioCall, apchRadioCall, depRadioCall, freqUse, freq );
            } while ( twr3.moveToNext() );
        }

        Cursor twr7 = result[ 4 ];
        if ( twr7.moveToFirst() ) {
            do {
                String freqUse = twr7.getString( twr7.getColumnIndex( Tower7.SATELLITE_AIRPORT_FREQ_USE ) );
                String freq = twr7.getString( twr7.getColumnIndex( Tower7.SATELLITE_AIRPORT_FREQ ) );
                processFrequency( map, towerRadioCall, apchRadioCall, depRadioCall, freqUse, freq );
            } while ( twr7.moveToNext() );
        }

        Cursor aff3 = result[ 5 ];
        if ( aff3.moveToFirst() ) {
            do {
                String artcc = aff3.getString( aff3.getColumnIndex( Aff3.ARTCC_ID ) );
                String freq = aff3.getString( aff3.getColumnIndex( Aff3.SITE_FREQUENCY ) );
                String alt = aff3.getString( aff3.getColumnIndex( Aff3.FREQ_ALTITUDE ) );
                String extra = "("+alt+" altitude)";
                String type = aff3.getString( aff3.getColumnIndex( Aff3.FACILITY_TYPE ) );
                if ( !type.equals( "ARTCC" ) ) {
                    extra = aff3.getString( aff3.getColumnIndex( Aff3.SITE_LOCATION ) )
                            +" "+type+" "+extra;
                }
                addFrequencyToMap( map, DataUtils.decodeArtcc( artcc ), freq, extra );
            } while ( aff3.moveToNext() );
        }

        if ( !map.isEmpty() ) {
            TextView tv = (TextView) findViewById( R.id.airport_comm_label );
            tv.setVisibility( View.VISIBLE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.airport_comm_details );
            layout.setVisibility( View.VISIBLE );
            String lastKey = null;
            for ( String key : map.keySet() ) {
                for ( Pair<String, String> pair : map.get( key ) ) {
                    if ( !key.equals( lastKey ) ) {
                        addRow( layout, key, pair.first, pair.second );
                        lastKey = key;
                    } else {
                        addRow( layout, "", pair.first, pair.second );
                    }
                }
            }
        }
    }

    protected void processFrequency( Map<String, ArrayList<Pair<String, String>>> map,
                                     String towerRadioCall, String apchRadioCall,
                                     String depRadioCall, String freqUse, String freq ) {
        boolean found = false;
        String extra = "";

        int i = 0;
        while ( i < freq.length() ) {
            char c = freq.charAt( i );
            if ( ( c >= '0' && c <= '9' ) || c == '.' ) {
                ++i;
                continue;
            }
            extra = freq.substring( i );
            freq = freq.substring( 0, i );
            break;
        }
        if ( freqUse.contains( "LCL" ) || freqUse.contains( "LC/P" ) ) {
            addFrequencyToMap( map, towerRadioCall+" Tower", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "GND" ) ) {
            addFrequencyToMap( map, towerRadioCall+" Ground", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "APCH" ) || freqUse.contains( "ARRIVAL" ) ) {
            addFrequencyToMap( map, apchRadioCall+" Approach", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "DEP" ) ) {
            addFrequencyToMap( map, depRadioCall+" Departure", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "CD" ) || freqUse.contains( "CLNC" ) ) {
            addFrequencyToMap( map, "Clearance Delivery", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "CLASS B" ) ) {
            addFrequencyToMap( map, "Class B", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "CLASS C" ) ) {
            addFrequencyToMap( map, "Class C", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "ATIS" ) ) {
            if ( freqUse.contains( "D-ATIS" ) ) {
                addFrequencyToMap( map, "D-ATIS", freq, extra );
            } else {
                addFrequencyToMap( map, "ATIS", freq, extra );
            }
            found = true;
        }
        if ( freqUse.contains( "RADAR" ) || freqUse.contains( "RDR" ) ) {
            addFrequencyToMap( map, "Radar", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "TRSA" ) ) {
            addFrequencyToMap( map, "TRSA", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "TAXI CLNC" ) ) {
            addFrequencyToMap( map, "Taxi Clearance", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "EMERG" ) ) {
            addFrequencyToMap( map, "Emergency", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "VFR-ADV" ) ) {
            addFrequencyToMap( map, "VFR Advisory", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "VFR ADZY" ) ) {
            addFrequencyToMap( map, "VFR Advisory", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "SFA" ) ) {
            addFrequencyToMap( map, "Single Freq Approach", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "PTC" ) ) {
            addFrequencyToMap( map, "PTC", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "PAR" ) ) {
            addFrequencyToMap( map, "PAR", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "AFIS" ) ) {
            addFrequencyToMap( map, "AFIS", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "PMSV" ) ) {
            addFrequencyToMap( map, "Operations", freq, extra );
            found = true;
        }
        if ( freqUse.contains( "IC" ) ) {
            addFrequencyToMap( map, "IC", freq, extra );
            found = true;
        }

        if ( !found ) {
            // Not able to recognize any token so show it as is
            addFrequencyToMap( map, freqUse, freq, extra );
        }
    }

    protected void showAtcHours( Cursor[] result ) {
        TreeMap<String, String> hoursMap = new TreeMap<>();

        Cursor twr2 = result[ 13 ];
        if ( twr2.moveToFirst() ) {
            do {
                String primaryAppHours = twr2.getString(
                        twr2.getColumnIndex( Tower2.PRIMARY_APPROACH_HOURS ) );
                if ( !primaryAppHours.isEmpty() ) {
                    hoursMap.put( "Primary approach", primaryAppHours );
                }
                String secondaryAppHours = twr2.getString(
                        twr2.getColumnIndex( Tower2.SECONDARY_APPROACH_HOURS ) );
                if ( !secondaryAppHours.isEmpty() ) {
                    hoursMap.put( "Secondary approach", secondaryAppHours );
                }
                String primaryDepHours = twr2.getString(
                        twr2.getColumnIndex( Tower2.PRIMARY_DEPARTURE_HOURS ) );
                if ( !primaryDepHours.isEmpty() ) {
                    hoursMap.put( "Primary departure", primaryDepHours );
                }
                String secondaryDepHours = twr2.getString(
                        twr2.getColumnIndex( Tower2.SECONDARY_DEPARTURE_HOURS ) );
                if ( !secondaryDepHours.isEmpty() ) {
                    hoursMap.put( "Secondary departure", secondaryDepHours );
                }
                String towerHours = twr2.getString(
                        twr2.getColumnIndex( Tower2.CONTROL_TOWER_HOURS ) );
                if ( !towerHours.isEmpty() ) {
                    hoursMap.put( "Control tower", towerHours );
                }
            } while ( twr2.moveToNext() );
        }

        Cursor twr9 = result[ 12 ];
        if ( twr9.moveToFirst() ) {
            String atisHours = twr9.getString( twr9.getColumnIndex( Tower9.ATIS_HOURS ) );
            if ( atisHours != null && !atisHours.isEmpty() ) {
                hoursMap.put( "ATIS", atisHours );
            }
        }

        if ( !hoursMap.isEmpty() ) {
            TextView tv = (TextView) findViewById( R.id.atc_hours_label );
            tv.setVisibility( View.VISIBLE );
            LinearLayout layout = (LinearLayout) findViewById( R.id.atc_hours_details );
            layout.setVisibility( View.VISIBLE );
            for ( String key : hoursMap.keySet() ) {
                addRow( layout, key, formatHours( hoursMap.get( key ) ) );
            }
        }
    }

    protected void showAtcPhones( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.atc_phones_details );

        Cursor main = result[ 6 ];
        if ( main.moveToFirst() ) {
            String phone = main.getString( main.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, "Command center", phone );
        }

        Cursor region = result[ 7 ];
        if ( region.moveToFirst() ) {
            String facility = region.getString( region.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String phone = region.getString( region.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, DataUtils.decodeFaaRegion( facility )+" region", phone );
        }

        Cursor artcc = result[ 8 ];
        if ( artcc.moveToFirst() ) {
            String facility = artcc.getString( artcc.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String phone = artcc.getString( artcc.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, DataUtils.decodeArtcc( facility ), phone );
            phone = artcc.getString( artcc.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            if ( phone != null && !phone.isEmpty() ) {
                addPhoneRow( layout, "", phone );
            }
        }

        Cursor tracon = result[ 9 ];
        if ( tracon != null && tracon.moveToFirst() ) {
            String faaCode = tracon.getString( tracon.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String type = tracon.getString( tracon.getColumnIndex( AtcPhones.FACILITY_TYPE ) );
            String name = DataUtils.getAtcFacilityName( faaCode+":"+type );

            String phone = tracon.getString( tracon.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, name+" TRACON", phone );
            phone = tracon.getString( tracon.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            if ( phone != null && !phone.isEmpty() ) {
                addPhoneRow( layout, "", phone );
            }
        }

        tracon = result[ 10 ];
        if ( tracon != null && tracon.moveToFirst() ) {
            String faaCode = tracon.getString( tracon.getColumnIndex( AtcPhones.FACILITY_ID ) );
            String type = tracon.getString( tracon.getColumnIndex( AtcPhones.FACILITY_TYPE ) );
            String name = DataUtils.getAtcFacilityName( faaCode+":"+type );

            String phone = tracon.getString( tracon.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, name+" TRACON", phone );
            phone = tracon.getString( tracon.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            if ( phone != null && !phone.isEmpty() ) {
                addPhoneRow( layout, "", phone );
            }
        }

        Cursor atct = result[ 11 ];
        if ( atct.moveToFirst() ) {
            Cursor tower1 = result[ 1 ];
            String name = tower1.getString( tower1.getColumnIndex( Tower1.RADIO_CALL_TOWER ) );
            String phone = atct.getString( atct.getColumnIndex( AtcPhones.DUTY_OFFICE_PHONE ) );
            addPhoneRow( layout, name+" Tower", phone );
            phone = atct.getString( atct.getColumnIndex( AtcPhones.BUSINESS_PHONE ) );
            if ( phone != null && !phone.isEmpty() ) {
                addPhoneRow( layout, "", phone );
            }
        }

        Cursor twr9 = result[ 12 ];
        if ( twr9.moveToFirst() ) {
            do {
                String atisPhone = twr9.getString( twr9.getColumnIndex( Tower9.ATIS_PHONE ) );
                if ( !atisPhone.isEmpty() ) {
                    addPhoneRow( layout, "ATIS", atisPhone );
                }
            } while ( twr9.moveToNext() );
        }
    }

    protected void showRemarks( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.comm_remarks_layout );

        Cursor twr6 = result[ 3 ];
        if ( twr6.moveToFirst() ) {
            do {
                String remark = twr6.getString( twr6.getColumnIndex( Tower6.REMARK_TEXT ) );
                addBulletedRow( layout, remark );
            } while ( twr6.moveToNext() );
        }

        Cursor twr9 = result[ 12 ];
        if ( twr9.moveToFirst() ) {
            do {
                String remark = twr9.getString( twr9.getColumnIndex( Tower9.ATIS_PURPOSE ) );
                if ( !remark.isEmpty() ) {
                    addBulletedRow( layout, remark );
                }
            } while ( twr9.moveToNext() );
        }

        Cursor twr4 = result[ 14 ];
        if ( twr4.moveToFirst() ) {
            String services = "";
            do {
                if ( !services.isEmpty() ) {
                    services = services.concat( ", " );
                }
                services = services.concat( twr4.getString(
                        twr4.getColumnIndex( Tower4.MASTER_AIRPORT_SERVICES ) ) ).trim();
            } while ( twr4.moveToNext() );
            addBulletedRow( layout, "Services to satellite airports: ".concat( services ) );
        }

        addBulletedRow( layout, "Facilities can be contacted by phone through the"
                +" regional duty officer during non-business hours." );
    }

    protected String formatHours( String hours ) {
        return hours.equals( "24" )? "24 Hr" : hours;
    }

    protected void addFrequencyToMap( Map<String, ArrayList<Pair<String, String>>> map,
            String key, String freq, String extra ) {
        ArrayList<Pair<String, String>> list = map.get( key );
        if ( list == null ) {
            list = new ArrayList<>();
        }
        list.add( Pair.create( FormatUtils.formatFreq( Float.valueOf( freq ) ), extra.trim() ) );
        map.put( key, list );
    }

    private final class CommunicationsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 15 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Tower1.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" },
                    Tower1.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 1 ] = c;

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower3.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, Tower3.MASTER_AIRPORT_FREQ_USE, null );
            cursors[ 2 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower6.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower3.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 3 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower7.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower7.SATELLITE_AIRPORT_SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );
            cursors[ 4 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Aff3.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Aff3.IFR_FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, null, null );
            cursors[ 5 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "MAIN", "MAIN" }, null, null, null, null );
            cursors[ 6 ] = c;

            String faaRegion = apt.getString( apt.getColumnIndex( Airports.REGION_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "REGION", faaRegion }, null, null, null, null );
            cursors[ 7 ] = c;

            String artccId = apt.getString( apt.getColumnIndex( Airports.BOUNDARY_ARTCC_ID ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "ARTCC", artccId }, null, null, null, null );
            cursors[ 8 ] = c;

            Cursor twr1 = cursors[ 1 ];
            if ( twr1.moveToFirst() ) {
                String apchName = twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_APCH_PRIMARY ) );
                String[] apchId = DataUtils.getAtcFacilityId( apchName );
                if ( apchId == null ) {
                    apchName = twr1.getString( twr1.getColumnIndex(
                            Tower1.RADIO_CALL_APCH_SECONDARY ) );
                    apchId = DataUtils.getAtcFacilityId( apchName );
                }
                if ( apchId != null ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( AtcPhones.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" },
                            "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                            new String[] { apchId[ 1 ], apchId[ 0 ] }, null, null, null, null );
                    cursors[ 9 ] = c;
                }

                String depName = twr1.getString( twr1.getColumnIndex(
                        Tower1.RADIO_CALL_DEP_PRIMARY ) );
                String[] depId = DataUtils.getAtcFacilityId( depName );
                if ( depId == null ) {
                    depName = twr1.getString( twr1.getColumnIndex(
                            Tower1.RADIO_CALL_DEP_SECONDARY ) );
                    depId = DataUtils.getAtcFacilityId( depName );
                }
                if ( depId != null ) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables( AtcPhones.TABLE_NAME );
                    c = builder.query( db, new String[] { "*" },
                            "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                            new String[] { depId[ 0 ], depId[ 1 ] }, null, null, null, null );
                    cursors[ 10 ] = c;
                }
            }

            builder = new SQLiteQueryBuilder();
            builder.setTables( AtcPhones.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    "("+AtcPhones.FACILITY_TYPE+"=? AND "+AtcPhones.FACILITY_ID+"=?)",
                    new String[] { "ATCT", faaCode }, null, null, null, null );
            cursors[ 11 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower9.TABLE_NAME );
            c = builder.query( db, new String[] { "*" },
                    Tower9.FACILITY_ID+"=?",
                    new String[] { faaCode }, null, null, Tower9.ATIS_SERIAL_NO, null );
            cursors[ 12 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower2.TABLE_NAME );
            c = builder.query( db, new String[]{ "*" },
                    Tower2.FACILITY_ID + "=?",
                    new String[]{ faaCode }, null, null, null, null );
            cursors[ 13 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower4.TABLE_NAME );
            c = builder.query( db, new String[]{ "*" },
                    Tower4.FACILITY_ID + "=?",
                    new String[]{ faaCode }, null, null, null, null );
            cursors[ 14 ] = c;

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
