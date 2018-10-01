package com.fincatto.documentofiscal.nfe400.webservices;

import java.util.Arrays;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe.NFeConfig;
import com.fincatto.documentofiscal.nfe310.webservices.gerado.CadConsultaCadastro2Stub;
import com.fincatto.documentofiscal.nfe310.webservices.gerado.CadConsultaCadastro2Stub.NfeCabecMsg;
import com.fincatto.documentofiscal.nfe310.webservices.gerado.CadConsultaCadastro2Stub.NfeCabecMsgE;
import com.fincatto.documentofiscal.nfe400.classes.NFAutorizador400;
import com.fincatto.documentofiscal.nfe400.classes.cadastro.NFConsultaCadastro;
import com.fincatto.documentofiscal.nfe400.classes.cadastro.NFInfoConsultaCadastro;
import com.fincatto.documentofiscal.nfe400.classes.cadastro.NFRetornoConsultaCadastro;
import com.fincatto.documentofiscal.nfe400.webservices.consultacadastro.CadConsultaCadastro4Stub;
import com.fincatto.documentofiscal.nfe400.webservices.consultacadastro.CadConsultaCadastro4Stub.NfeDadosMsg;
import com.fincatto.documentofiscal.transformers.DFRegistryMatcher;

class WSConsultaCadastro {
    private static final Logger LOG = LoggerFactory.getLogger(WSConsultaCadastro.class);
    private static final String NOME_SERVICO = "CONS-CAD";
    private static final String VERSAO_SERVICO = "2.00";
    private final NFeConfig config;

    WSConsultaCadastro(final NFeConfig config) {
        this.config = config;
    }

    NFRetornoConsultaCadastro consultaCadastro(final String cnpj, final DFUnidadeFederativa uf) throws Exception {
        final NFConsultaCadastro dadosConsulta = this.getDadosConsulta(cnpj, uf);
        final String xmlConsulta = dadosConsulta.toString();
        WSConsultaCadastro.LOG.debug(xmlConsulta);

        final OMElement omElementConsulta = AXIOMUtil.stringToOM(xmlConsulta);
        final OMElement resultado = this.efetuaConsulta(uf, omElementConsulta);

        final String retornoConsulta = resultado.toString();
        WSConsultaCadastro.LOG.debug(retornoConsulta);
        return new Persister(new DFRegistryMatcher(), new Format(0)).read(NFRetornoConsultaCadastro.class, retornoConsulta);
    }

    private OMElement efetuaConsulta(final DFUnidadeFederativa uf, final OMElement omElementConsulta) throws Exception {

        final NFAutorizador400 autorizador = NFAutorizador400.valueOfCodigoUF(uf);
        if (autorizador == null) {
            throw new IllegalStateException(String.format("UF %s nao possui autorizador para este servico", uf.getDescricao()));
        }
        final String url = autorizador.getConsultaCadastro(this.config.getAmbiente());
        WSConsultaCadastro.LOG.debug(String.format("Endpoint: %s", url));

        // estados que ainda nao possuem versao 4
        if (Arrays.asList(DFUnidadeFederativa.AM, DFUnidadeFederativa.MG, DFUnidadeFederativa.PE).contains(uf)) {
            final CadConsultaCadastro2Stub.NfeCabecMsg cabec = new NfeCabecMsg();
            cabec.setCUF(uf.getCodigoIbge());
            cabec.setVersaoDados(WSConsultaCadastro.VERSAO_SERVICO);

            final NfeCabecMsgE cabecE = new NfeCabecMsgE();
            cabecE.setNfeCabecMsg(cabec);

            final com.fincatto.documentofiscal.nfe310.webservices.gerado.CadConsultaCadastro2Stub.NfeDadosMsg nfeDadosMsg = new com.fincatto.documentofiscal.nfe310.webservices.gerado.CadConsultaCadastro2Stub.NfeDadosMsg();
            nfeDadosMsg.setExtraElement(omElementConsulta);
            return new CadConsultaCadastro2Stub(url).consultaCadastro2(nfeDadosMsg, cabecE).getExtraElement();
        } else {
            final NfeDadosMsg nfeDadosMsg_type0 = new NfeDadosMsg();
            nfeDadosMsg_type0.setExtraElement(omElementConsulta);
            return new CadConsultaCadastro4Stub(url).consultaCadastro(nfeDadosMsg_type0).getExtraElement();
        }

    }

    private NFConsultaCadastro getDadosConsulta(final String cnpj, final DFUnidadeFederativa uf) {
        final NFConsultaCadastro consulta = new NFConsultaCadastro();
        consulta.setVersao(WSConsultaCadastro.VERSAO_SERVICO);
        consulta.setConsultaCadastro(new NFInfoConsultaCadastro());
        consulta.getConsultaCadastro().setCnpj(cnpj);
        consulta.getConsultaCadastro().setServico(WSConsultaCadastro.NOME_SERVICO);
        consulta.getConsultaCadastro().setUf(uf.getCodigo());
        return consulta;
    }
}
