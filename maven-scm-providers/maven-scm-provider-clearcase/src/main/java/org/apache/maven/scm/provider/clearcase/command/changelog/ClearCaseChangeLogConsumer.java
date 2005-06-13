package org.apache.maven.scm.provider.clearcase.command.changelog;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.ChangeFile;

import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.logging.Logger;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class ClearCaseChangeLogConsumer
    implements StreamConsumer
{
    private Logger logger;

    /**
     * Formatter used to parse Clearcase date/timestamp.
     */
    private static final SimpleDateFormat CLEARCASE_TIMESTAMP_FORMAT = new SimpleDateFormat( "yyyyMMdd.HHmmss" );

    private static final String NAME_TAG = "NAME:";

    private static final String USER_TAG = "USER:";

    private static final String DATE_TAG = "DATE:";

    private static final String COMMENT_TAG = "COMM:";

    /**
     * Custom date/time formatter.  Rounds ChangeLogEntry times to the nearest
     * minute.
     */
    private static final SimpleDateFormat ENTRY_KEY_TIMESTAMP_FORMAT = new SimpleDateFormat( "yyyyMMddHHmm" );

    private List entries = new ArrayList();

    // state machine constants for reading clearcase lshistory command output
    /**
     * expecting file information
     */
    private static final int GET_FILE = 1;

    /**
     * expecting date
     */
    private static final int GET_DATE = 2;

    /**
     * expecting comments
     */
    private static final int GET_COMMENT = 3;

    /**
     * current status of the parser
     */
    private int status = GET_FILE;

    /**
     * the current log entry being processed by the parser
     */
    private ChangeSet currentChange = null;

    /**
     * the current file being processed by the parser
     */
    private ChangeFile currentFile = null;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public ClearCaseChangeLogConsumer( Logger logger )
    {
        this.logger = logger;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getModifications()
    {
        return entries;
    }

    // ----------------------------------------------------------------------
    // StreamConsumer Implementation
    // ----------------------------------------------------------------------

    public void consumeLine( String line )
    {
        switch ( getStatus() )
        {
            case GET_FILE:
                processGetFile( line );
                break;
            case GET_DATE:
                processGetDate( line );
                break;
            case GET_COMMENT:
                processGetCommentAndUser( line );
                break;
            default:
                logger.warn( "Unknown state: " + status );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Process the current input line in the Get File state.
     *
     * @param line a line of text from the clearcase log output
     */
    private void processGetFile( String line )
    {
        if ( line.startsWith( NAME_TAG ) )
        {
            setCurrentChange( new ChangeSet() );
            setCurrentFile( new ChangeFile( line.substring( NAME_TAG.length(), line.length() ) ) );
            setStatus( GET_DATE );
        }
    }

    /**
     * Process the current input line in the Get Date state.
     *
     * @param line a line of text from the clearcase log output
     */
    private void processGetDate( String line )
    {
        if ( line.startsWith( DATE_TAG ) )
        {
            try
            {
                getCurrentChange().setDate( CLEARCASE_TIMESTAMP_FORMAT.parse( line.substring( DATE_TAG.length() ) ) );
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }
            setStatus( GET_COMMENT );
        }
    }

    /**
     * Process the current input line in the Get Comment state.
     *
     * @param line a line of text from the clearcase log output
     */
    private void processGetCommentAndUser( String line )
    {
        if ( line.startsWith( COMMENT_TAG ) )
        {
            String comm = line.substring( COMMENT_TAG.length() );

            getCurrentChange().setComment( getCurrentChange().getComment() + comm + "\n" );
        }
        else if ( line.startsWith( USER_TAG ) )
        {
            getCurrentChange().setAuthor( line.substring( USER_TAG.length() ) );

            // add entry, and set state to get file
            getCurrentChange().setFile( getCurrentFile() );

            entries.add( getCurrentChange() );

            setStatus( GET_FILE );
        }
        else
        {
            // keep gathering comments
            getCurrentChange().setComment( getCurrentChange().getComment() + line + "\n" );
        }
    }

    /**
     * Getter for property currentFile.
     *
     * @return Value of property currentFile.
     */
    private ChangeFile getCurrentFile()
    {
        return currentFile;
    }

    /**
     * Setter for property currentFile.
     *
     * @param currentFile New value of property currentFile.
     */
    private void setCurrentFile( ChangeFile currentFile )
    {
        this.currentFile = currentFile;
    }

    /**
     * Getter for property currentChange.
     *
     * @return Value of property currentChange.
     */
    private ChangeSet getCurrentChange()
    {
        return currentChange;
    }

    /**
     * Setter for property currentLogEntry.
     *
     * @param currentLogEntry New value of property currentLogEntry.
     */
    private void setCurrentChange( ChangeSet currentChange )
    {
        this.currentChange = currentChange;
    }

    /**
     * Getter for property status.
     *
     * @return Value of property status.
     */
    private int getStatus()
    {
        return status;
    }

    /**
     * Setter for property status.
     *
     * @param status New value of property status.
     */
    private void setStatus( int status )
    {
        this.status = status;
    }
}