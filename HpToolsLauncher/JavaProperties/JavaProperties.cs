/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Collections;

namespace HpToolsLauncher
{

    /// <summary>
    ///  This class is a direct port to C# of the java properties class
    /// </summary>
    public class JavaProperties : Dictionary<string, string>
    {

        protected JavaProperties defaults;

        /// <summary>
        /// Creates an empty property list with no default values.
        /// </summary>
        public JavaProperties()
            : this(null)
        {

        }

        /// <summary>
        /// Creates an empty property list with the specified defaults.
        /// </summary>
        /// <param name="defaults"></param>
        public JavaProperties(JavaProperties defaults)
        {
            this.defaults = defaults;
        }



        /// <summary>
        /// loads properties
        /// </summary>
        /// <param name="reader"></param>
        public void Load(TextReader reader)
        {
            LoadInternal(new LineReader(reader));
        }


        /// <summary>
        /// loads properties from a file
        /// </summary>
        /// <param name="fullpath"></param>
        public void Load(string fullpath)
        {
            using (FileStream s = File.OpenRead(fullpath))
            {
                LoadInternal(new LineReader(s));
            }
        }

        /// <summary>
        /// loads properties from stream
        /// </summary>
        /// <param name="inStream"></param>
        public void Load(Stream inStream)
        {
            LoadInternal(new LineReader(inStream));
        }

        private void LoadInternal(LineReader lr)
        {
            char[] convtBuf = new char[1024];
            int limit;
            int keyLen;
            int valueStart;
            char c;
            bool hasSep;
            bool precedingBackslash;

            while ((limit = lr.readLine()) >= 0)
            {
                c = '\0';
                keyLen = 0;
                valueStart = limit;
                hasSep = false;

                //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
                precedingBackslash = false;
                while (keyLen < limit)
                {
                    c = lr.lineBuf[keyLen];
                    //need check if escaped.
                    if ((c == '=' || c == ':') && !precedingBackslash)
                    {
                        valueStart = keyLen + 1;
                        hasSep = true;
                        break;
                    }
                    else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash)
                    {
                        valueStart = keyLen + 1;
                        break;
                    }
                    if (c == '\\')
                    {
                        precedingBackslash = !precedingBackslash;
                    }
                    else
                    {
                        precedingBackslash = false;
                    }
                    keyLen++;
                }
                while (valueStart < limit)
                {
                    c = lr.lineBuf[valueStart];
                    if (c != ' ' && c != '\t' && c != '\f')
                    {
                        if (!hasSep && (c == '=' || c == ':'))
                        {
                            hasSep = true;
                        }
                        else
                        {
                            break;
                        }
                    }
                    valueStart++;
                }
                String key = LoadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = LoadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                this[key] = value;
            }
        }

        class LineReader
        {
            public LineReader(Stream inStream)
            {
                this.inStream = inStream;
                inByteBuf = new byte[8192];
            }

            public LineReader(TextReader reader)
            {
                this.reader = reader;
                inCharBuf = new char[8192];
            }

            byte[] inByteBuf;
            char[] inCharBuf;
            internal char[] lineBuf = new char[1024];
            int inLimit = 0;
            int inOff = 0;
            Stream inStream;
            TextReader reader;

            public int readLine()
            {
                int len = 0;
                char c = '\0';

                bool skipWhiteSpace = true;
                bool isCommentLine = false;
                bool isNewLine = true;
                bool appendedLineBegin = false;
                bool precedingBackslash = false;
                bool skipLF = false;

                while (true)
                {
                    if (inOff >= inLimit)
                    {
                        inLimit = (inStream == null) ? reader.Read(inCharBuf, 0, inCharBuf.Length)
                                                  : inStream.Read(inByteBuf, 0, inByteBuf.Length);
                        inOff = 0;
                        if (inLimit <= 0)
                        {
                            if (len == 0 || isCommentLine)
                            {
                                return -1;
                            }
                            return len;
                        }
                    }
                    if (inStream != null)
                    {
                        //The line below is equivalent to calling a
                        //ISO8859-1 decoder.
                        c = (char)(0xff & inByteBuf[inOff++]);
                    }
                    else
                    {
                        c = inCharBuf[inOff++];
                    }
                    if (skipLF)
                    {
                        skipLF = false;
                        if (c == '\n')
                        {
                            continue;
                        }
                    }
                    if (skipWhiteSpace)
                    {
                        if (c == ' ' || c == '\t' || c == '\f')
                        {
                            continue;
                        }
                        if (!appendedLineBegin && (c == '\r' || c == '\n'))
                        {
                            continue;
                        }
                        skipWhiteSpace = false;
                        appendedLineBegin = false;
                    }
                    if (isNewLine)
                    {
                        isNewLine = false;
                        if (c == '#' || c == '!')
                        {
                            isCommentLine = true;
                            continue;
                        }
                    }

                    if (c != '\n' && c != '\r')
                    {
                        lineBuf[len++] = c;
                        if (len == lineBuf.Length)
                        {
                            int newLength = lineBuf.Length * 2;
                            if (newLength < 0)
                            {
                                newLength = Int32.MaxValue;
                            }
                            char[] buf = new char[newLength];
                            Array.Copy(lineBuf, 0, buf, 0, lineBuf.Length);
                            lineBuf = buf;
                        }
                        //flip the preceding backslash flag
                        if (c == '\\')
                        {
                            precedingBackslash = !precedingBackslash;
                        }
                        else
                        {
                            precedingBackslash = false;
                        }
                    }
                    else
                    {
                        // reached EOL
                        if (isCommentLine || len == 0)
                        {
                            isCommentLine = false;
                            isNewLine = true;
                            skipWhiteSpace = true;
                            len = 0;
                            continue;
                        }
                        if (inOff >= inLimit)
                        {
                            inLimit = (inStream == null)
                                      ? reader.Read(inCharBuf, 0, inCharBuf.Length)
                                      : inStream.Read(inByteBuf, 0, inByteBuf.Length);
                            inOff = 0;
                            if (inLimit <= 0)
                            {
                                return len;
                            }
                        }
                        if (precedingBackslash)
                        {
                            len -= 1;
                            //skip the leading whitespace characters in following line
                            skipWhiteSpace = true;
                            appendedLineBegin = true;
                            precedingBackslash = false;
                            if (c == '\r')
                            {
                                skipLF = true;
                            }
                        }
                        else
                        {
                            return len;
                        }
                    }
                }
            }
        }


        /// <summary>
        ///  Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original forms
        /// </summary>
        /// <param name="in1"></param>
        /// <param name="off"></param>
        /// <param name="len"></param>
        /// <param name="convtBuf"></param>
        /// <returns></returns>
        private String LoadConvert(char[] in1, int off, int len, char[] convtBuf)
        {
            if (convtBuf.Length < len)
            {
                int newLen = len * 2;
                if (newLen < 0)
                {
                    newLen = Int32.MaxValue;
                }
                convtBuf = new char[newLen];
            }
            char aChar;
            char[] out1 = convtBuf;
            int outLen = 0;
            int end = off + len;

            while (off < end)
            {
                aChar = in1[off++];
                if (aChar == '\\')
                {
                    aChar = in1[off++];
                    if (aChar == 'u')
                    {
                        // Read the xxxx
                        int value = 0;
                        for (int i = 0; i < 4; i++)
                        {
                            aChar = in1[off++];
                            switch (aChar)
                            {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw new ArgumentException(
                                                 "Malformed \\uxxxx encoding.");
                            }
                        }
                        out1[outLen++] = (char)value;
                    }
                    else
                    {
                        if (aChar == 't') aChar = '\t';
                        else if (aChar == 'r') aChar = '\r';
                        else if (aChar == 'n') aChar = '\n';
                        else if (aChar == 'f') aChar = '\f';
                        out1[outLen++] = aChar;
                    }
                }
                else
                {
                    out1[outLen++] = aChar;
                }
            }
            return new String(out1, 0, outLen);
        }


        /// <summary>
        /// Converts unicodes to encoded &#92;uxxxx and escapes special characters with a preceding slash
        /// </summary>
        /// <param name="theString"></param>
        /// <param name="escapeSpace"></param>
        /// <param name="escapeUnicode"></param>
        /// <returns></returns>
        private String SaveConvert(String theString,
                                   bool escapeSpace,
                                   bool escapeUnicode)
        {
            int len = theString.Length;
            int bufLen = len * 2;
            if (bufLen < 0)
            {
                bufLen = Int32.MaxValue;
            }
            StringBuilder outBuffer = new StringBuilder(bufLen);

            for (int x = 0; x < len; x++)
            {
                char aChar = theString[x];
                // Handle common case first, selecting largest block that
                // avoids the specials below
                if ((aChar > 61) && (aChar < 127))
                {
                    if (aChar == '\\')
                    {
                        outBuffer.Append('\\'); outBuffer.Append('\\');
                        continue;
                    }
                    outBuffer.Append(aChar);
                    continue;
                }
                switch (aChar)
                {
                    case ' ':
                        if (x == 0 || escapeSpace)
                            outBuffer.Append('\\');
                        outBuffer.Append(' ');
                        break;
                    case '\t': outBuffer.Append('\\'); outBuffer.Append('t');
                        break;
                    case '\n': outBuffer.Append('\\'); outBuffer.Append('n');
                        break;
                    case '\r': outBuffer.Append('\\'); outBuffer.Append('r');
                        break;
                    case '\f': outBuffer.Append('\\'); outBuffer.Append('f');
                        break;
                    case '=': // Fall through
                    case ':': // Fall through
                    case '#': // Fall through
                    case '!':
                        outBuffer.Append('\\'); outBuffer.Append(aChar);
                        break;
                    default:
                        if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode)
                        {
                            outBuffer.Append('\\');
                            outBuffer.Append('u');
                            outBuffer.Append(ToHex((aChar >> 12) & 0xF));
                            outBuffer.Append(ToHex((aChar >> 8) & 0xF));
                            outBuffer.Append(ToHex((aChar >> 4) & 0xF));
                            outBuffer.Append(ToHex(aChar & 0xF));
                        }
                        else
                        {
                            outBuffer.Append(aChar);
                        }
                        break;
                }
            }
            return outBuffer.ToString();
        }

        private static void WriteComments(System.IO.TextWriter bw, String comments)
        {
            bw.Write("#");
            int len = comments.Length;
            int current = 0;
            int last = 0;
            char[] uu = new char[6];
            uu[0] = '\\';
            uu[1] = 'u';
            while (current < len)
            {
                char c = comments[current];
                if (c > '\u00ff' || c == '\n' || c == '\r')
                {
                    if (last != current)
                        bw.Write(comments.Substring(last, current));
                    if (c > '\u00ff')
                    {
                        uu[2] = ToHex((c >> 12) & 0xf);
                        uu[3] = ToHex((c >> 8) & 0xf);
                        uu[4] = ToHex((c >> 4) & 0xf);
                        uu[5] = ToHex(c & 0xf);
                        bw.Write(new String(uu));
                    }
                    else
                    {
                        bw.Write(bw.NewLine);
                        if (c == '\r' &&
                            current != len - 1 &&
                            comments[current + 1] == '\n')
                        {
                            current++;
                        }
                        if (current == len - 1 ||
                            (comments[current + 1] != '#' &&
                            comments[current + 1] != '!'))
                            bw.Write("#");
                    }
                    last = current + 1;
                }
                current++;
            }
            if (last != current)
                bw.Write(comments.Substring(last, current) + bw.NewLine);

        }

        //@Deprecated
        public void Save(Stream out1, String comments)
        {
            try
            {
                Store(out1, comments);
            }
            catch (IOException e)
            {
            }
        }

        public void Save(String fileName, String comments)
        {
            Store(fileName, comments);
        }

        /// <summary>
        /// saves the properties
        /// </summary>
        /// <param name="writer"></param>
        /// <param name="comments"></param>
        public void Store(TextWriter writer, String comments)
        {
            StoreInternal(writer, comments, false);
        }

        /// <summary>
        /// saves the properties to stream
        /// </summary>
        /// <param name="writer"></param>
        /// <param name="comments"></param>
        public void Store(Stream out1, String comments)
        {
            TextWriter t = new StreamWriter(out1, Encoding.GetEncoding("ISO-8859-1"));
            StoreInternal(t, comments, true);
        }

        /// <summary>
        /// saves the properties to file
        /// </summary>
        /// <param name="writer"></param>
        /// <param name="comments"></param>
        public void Store(string fullpath, String comments)
        {
            using (StreamWriter wr = new StreamWriter(fullpath, false, Encoding.GetEncoding("ISO-8859-1")))
            {
                StoreInternal(wr, comments, true);
            }
        }
        private void StoreInternal(TextWriter bw, String comments, bool escUnicode)
        {
            if (comments != null)
            {
                WriteComments(bw, comments);
            }
            bw.Write("#" + DateTime.Now.ToString() + bw.NewLine);
            {
                foreach (string key in Keys)
                {
                    String val = (string)this[key];
                    string key1 = SaveConvert(key, true, escUnicode);
                    /* No need to escape embedded and trailing spaces for value, hence
                     * pass false to flag.
                     */
                    val = SaveConvert(val, false, escUnicode);
                    bw.Write(key1 + "=" + val + bw.NewLine);
                }
            }
            bw.Flush();
        }

        /// <summary>
        ///  Convert a nibble to a hex character
        /// </summary>
        /// <param name="nibble">nibble to convert</param>
        /// <returns></returns>
        private static char ToHex(int nibble)
        {
            return hexDigit[(nibble & 0xF)];
        }

        private static char[] hexDigit = {
           '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
       };
    }

}


